package moe.pure.browser.browser.state

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

/** Only worker operations resolve directories or touch disk; constructing an owner performs no IO. */
internal class TabFileStore(context: Context) {
    private val app = context.applicationContext

    fun write(id: String, bytes: ByteArray): String {
        val name = id.takeIf { it.isNotEmpty() && it != "." && it != ".." &&
            it.all { c -> c.isLetterOrDigit() || c == '-' || c == '_' } }
            ?: UUID.nameUUIDFromBytes(id.toByteArray(Charsets.UTF_8)).toString()
        val root = roots().firstOrNull { ensureDirectory(it) }
            ?: throw IOException("Tab state directory is unavailable")
        return writeAtomic(File(root, name)) { it.write(bytes) }.absolutePath
    }

    fun read(path: String?): ByteArray? = owned(path)?.let { file ->
        try { AtomicFile(file).openRead().use { it.readBytes() } }
        catch (_: IOException) { null }
        catch (_: SecurityException) { null }
    }

    fun delete(path: String?) {
        owned(path)?.let { AtomicFile(it).delete() }
    }

    fun prune(referencedPaths: Collection<String?>) {
        val referenced = referencedPaths.mapNotNull { owned(it)?.absolutePath }.toHashSet()
        roots().forEach { root ->
            root.listFiles()?.forEach { child ->
                val baseName = child.name.removeSuffix(".bak").removeSuffix(".new")
                val base = owned(File(root, baseName).absolutePath)
                if (base != null && base.absolutePath !in referenced) AtomicFile(base).delete()
            }
        }
    }

    private fun owned(path: String?): File? {
        if (path.isNullOrEmpty()) return null
        return try {
            val candidate = File(path)
            val parent = candidate.parentFile?.canonicalFile ?: return null
            if (parent !in roots()) return null
            val expected = File(parent, candidate.name)
            if (expected.canonicalFile != expected) null else expected
        } catch (_: IOException) { null }
        catch (_: SecurityException) { null }
    }

    private fun roots(): List<File> = buildList {
        runCatching { app.getExternalFilesDir("tabs")?.canonicalFile }.getOrNull()?.let(::add)
        val internal = File(app.filesDir, "tabs").canonicalFile
        if (internal !in this) add(internal)
    }

    private fun ensureDirectory(file: File): Boolean = try {
        (file.isDirectory || file.mkdirs()) && file.isDirectory
    } catch (_: SecurityException) { false }

    private fun writeAtomic(file: File, writer: (OutputStream) -> Unit): File {
        if (file.canonicalFile != file) throw IOException("Tab state path leaves its owned directory")
        val atomic = AtomicFile(file)
        val output = atomic.startWrite()
        try {
            writer(output)
            atomic.finishWrite(output)
        } catch (error: Throwable) {
            atomic.failWrite(output)
            throw error
        }
        if (!file.isFile) throw IOException("Tab state was not committed")
        return file
    }
}
