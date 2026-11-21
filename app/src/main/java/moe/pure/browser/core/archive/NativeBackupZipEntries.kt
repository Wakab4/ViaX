package moe.pure.browser.core.archive

import moe.pure.browser.core.files.NativeStreams
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** d4's backup aliases, traversal and stream closing differ from e4's diagnostic ZIP helper. */
internal object NativeBackupZipEntries {
    fun append(file: File, alias: String?, zip: ZipOutputStream, output: BufferedOutputStream?) {
        if (!file.exists()) throw FileNotFoundException("${file.absolutePath} does not exist")
        val name = when {
            alias.isNullOrEmpty() -> file.name
            alias.endsWith('/') -> alias.substring(0, alias.length - 1)
            else -> alias
        }
        appendEntry(file, name, zip, output)
    }

    private fun appendEntry(file: File, name: String, zip: ZipOutputStream, output: BufferedOutputStream?) {
        if (file.isDirectory) {
            val children = file.listFiles() ?: return
            if (children.isEmpty()) {
                zip.putNextEntry(ZipEntry("$name/"))
                zip.closeEntry()
            } else for (child in children) appendEntry(child, "$name/${child.name}", zip, output)
            return
        }
        var source: FileInputStream? = null
        var input: BufferedInputStream? = null
        try {
            zip.putNextEntry(ZipEntry(name))
            source = FileInputStream(file)
            input = BufferedInputStream(source)
            val buffer = ByteArray(4096)
            while (true) {
                val size = input.read(buffer)
                if (size == -1) break
                output!!.write(buffer, 0, size)
            }
            output!!.flush()
            zip.closeEntry()
        } catch (error: Throwable) {
            NativeStreams.close(input)
            NativeStreams.close(source)
            throw error
        }
        NativeStreams.close(input)
        NativeStreams.close(source)
    }
}
