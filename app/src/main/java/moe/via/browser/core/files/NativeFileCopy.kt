package moe.via.browser.core.files

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** c1.a copies directly, keeping its destination deletion and partial-write behavior. */
internal object NativeFileCopy {
    fun copy(source: File?, destination: File?): Boolean {
        if (source == null || !source.canRead() || !source.exists() || destination == null) return false
        var target = destination
        if (target.exists()) {
            if (!target.canRead() || !target.canWrite()) return false
            if (target.isDirectory) {
                target = File(target, source.name)
                if (target.exists() && !target.delete()) return false
            } else if (!target.delete()) return false
        }
        return try {
            BufferedInputStream(FileInputStream(source)).use { input ->
                BufferedOutputStream(FileOutputStream(target)).use { output ->
                    val buffer = ByteArray(1024)
                    while (true) {
                        val size = input.read(buffer)
                        if (size <= 0) break
                        output.write(buffer, 0, size)
                        output.flush()
                    }
                }
            }
            true
        } catch (error: Exception) {
            error.printStackTrace()
            false
        }
    }
}
