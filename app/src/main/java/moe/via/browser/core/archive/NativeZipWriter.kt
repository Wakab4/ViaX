package moe.via.browser.core.archive

import moe.via.browser.core.logging.NativeLog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** e4.c/d counts successful top-level files, preserving direct writes and partial archives. */
internal object NativeZipWriter {
    fun write(target: File, files: List<File>): Boolean {
        if (files.isEmpty()) return false
        var count = 0
        val names = HashSet<String>()
        try {
            ZipOutputStream(FileOutputStream(target)).use { zip ->
                for (file in files) if (append(file.parent, file.name, zip, names)) count++
                zip.finish()
            }
        } catch (error: Exception) { NativeLog.error(null, error) }
        return count > 0
    }

    private fun append(parent: String?, name: String, zip: ZipOutputStream, names: MutableSet<String>): Boolean {
        val file = File(parent, name)
        if (!file.exists()) return false
        if (file.isFile) {
            val entryName = name.replace(File.separatorChar, '/')
            if (!names.add(entryName)) return true
            zip.putNextEntry(ZipEntry(entryName).apply { time = file.lastModified() })
            FileInputStream(file).use { source ->
                val buffer = ByteArray(4096)
                while (true) { val size = source.read(buffer); if (size == -1) break; zip.write(buffer, 0, size) }
            }
            zip.closeEntry()
            return true
        }
        val children = file.list()
        if (children == null || children.isEmpty()) {
            val entryName = name.replace(File.separatorChar, '/') + "/"
            if (!names.add(entryName)) return true
            zip.putNextEntry(ZipEntry(entryName).apply { time = file.lastModified() })
            zip.closeEntry()
            return true
        }
        var count = 0
        for (child in children) if (append(parent, name + File.separator + child, zip, names)) count++
        return count == children.size
    }
}
