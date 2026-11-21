package moe.pure.browser.core.archive

import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/** e4.a writes entries directly; the fallback DEX confirms its prefix check and close order. */
internal object NativeZipReader {
    fun extract(archive: File, destination: String): Boolean = try {
        FileInputStream(archive).use { extract(it, destination) }
    } catch (error: Exception) { error.printStackTrace(); false }

    fun extract(input: InputStream?, destination: String): Boolean = try {
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val file = File(destination, entry.name)
                if (!file.canonicalPath.startsWith(destination)) continue
                if (entry.isDirectory) file.mkdirs()
                else {
                    if (!file.exists()) { file.parentFile!!.mkdirs(); file.createNewFile() }
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(1024)
                        while (true) {
                            val size = zip.read(buffer)
                            if (size == -1) break
                            output.write(buffer, 0, size); output.flush()
                        }
                    }
                    if (entry.time > 0) file.setLastModified(entry.time)
                }
            }
        }
        true
    } catch (error: Exception) { error.printStackTrace(); false }
}
