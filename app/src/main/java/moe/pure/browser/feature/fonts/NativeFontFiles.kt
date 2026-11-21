package moe.pure.browser.feature.fonts

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/** z8.c1.b/e/a preserve direct writes, partial files and their distinct close-error returns. */
internal object NativeFontFiles {
    private fun directory(file: File?): Boolean {
        if (file == null) return false
        if (!file.exists()) return file.mkdirs()
        if (file.isDirectory) return true
        return file.delete() && file.mkdirs()
    }
    fun fromUri(context: Context, uri: Uri, target: File): String? {
        if (uri.authority == null) return null
        var path: String? = null
        try {
            val input = context.contentResolver.openInputStream(uri)
            try { path = write(input, target)?.path }
            catch (error: Throwable) {
                try { input?.close() } catch (close: Throwable) { error.addSuppressed(close) }
                throw error
            }
            input?.close()
        } catch (error: IOException) { error.printStackTrace() }
        return path
    }
    private fun write(input: InputStream?, target: File): File? {
        if (input == null || !directory(target.parentFile) || target.exists() && !target.delete()) return null
        return try {
            val output = FileOutputStream(target)
            try {
                val buffer = ByteArray(8192)
                while (true) { val read = input.read(buffer); if (read == -1) break; output.write(buffer, 0, read) }
                output.flush()
            } catch (error: Throwable) {
                try { output.close() } catch (close: Throwable) { error.addSuppressed(close) }
                throw error
            }
            try { output.close() } catch (error: IOException) { error.printStackTrace() }
            target
        } catch (error: IOException) { error.printStackTrace(); null }
    }
    fun copy(source: File, destination: File): Boolean {
        if (!source.canRead() || !source.exists()) return false
        var target = destination
        if (target.exists()) {
            if (!target.canRead() || !target.canWrite()) return false
            if (target.isDirectory) target = File(target, source.name)
            if (target.exists() && !target.delete()) return false
        }
        return try {
            BufferedInputStream(FileInputStream(source)).use { input ->
                BufferedOutputStream(FileOutputStream(target)).use { output ->
                    val buffer = ByteArray(1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read); output.flush()
                    }
                }
            }
            true
        } catch (error: Exception) { error.printStackTrace(); false }
    }
}
