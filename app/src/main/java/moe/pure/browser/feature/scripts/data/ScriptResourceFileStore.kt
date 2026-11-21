package moe.pure.browser.feature.scripts.data

import android.content.Context
import java.io.File
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/** p5.b selects its directory per data-source instance and keeps plain hashed cache paths. */
internal class ScriptResourceFileStore(context: Context) {
    private val root = File(context.getExternalFilesDir("gm") ?: File(context.filesDir, "gm"), "resources")
        .let { if (it.exists() || it.mkdirs()) it else null }

    fun file(url: String): File? = root?.takeIf { url.isNotEmpty() }?.let { File(it, name(url)) }
    fun fetch(url: String): ScriptResourceFetch = NativeScriptResourceDownload.fetch(url, file(url))
    fun prune(retained: Set<String>): Boolean {
        val directory = root ?: return false
        if (retained.isEmpty()) {
            val result = delete(directory)
            directory.mkdirs()
            return result
        }
        val names = retained.mapTo(HashSet(), ::name)
        val children = directory.listFiles() ?: return false
        if (children.isEmpty()) return false
        var failed = 0
        children.forEach { if (it.name !in names && !delete(it)) failed++ }
        return failed == 0
    }

    companion object {
        fun http(url: String) = url.length >= 4 && url.substring(0, 4).equals("http", true)
        fun delete(file: File?): Boolean {
            if (file == null || !file.exists()) return true
            if (file.isDirectory) file.listFiles()?.forEach { if (!delete(it)) return false }
            return file.delete()
        }
        private fun name(url: String): String = if (url.isEmpty()) "" else try {
            MessageDigest.getInstance("MD5").digest(url.toByteArray(Charset.defaultCharset()))
                .joinToString("") { Integer.toHexString(it.toInt() and 255).padStart(2, '0') }
        } catch (_: NoSuchAlgorithmException) { Integer.toHexString(url.hashCode()) }
    }
}
