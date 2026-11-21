package moe.pure.browser.feature.home.background

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import moe.pure.browser.core.files.NativeDirectories
import moe.pure.browser.core.graphics.NativeBitmapImages
import moe.pure.browser.core.graphics.NativeDisplaySize
import moe.pure.browser.core.logging.NativeLog
import java.io.File
import java.io.FileOutputStream

/** j9.a.a removes only the original background JPEG cache names. */
internal object NativeBackgroundFiles {
    fun cacheFile(context: Context) = File(NativeDirectories.appFiles(context, "content"), "background.jpg").absoluteFile

    fun prepare(context: Context, path: String?): Array<String> {
        if (path.isNullOrEmpty()) {
            NativeLog.warn(null, "Can not process with null or empty path")
            return emptyArray()
        }
        val source = File(path)
        if (!source.exists() || source.isDirectory) {
            NativeLog.warn(null, "Can not process with invalid file: $path")
            return emptyArray()
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        val display = NativeDisplaySize.real(context)
        val limit = maxOf(display.x, display.y)
        options.inSampleSize = NativeBitmapImages.sample(options.outWidth, options.outHeight, limit, limit)
        options.inJustDecodeBounds = false
        val bitmap = BitmapFactory.decodeFile(path, options)
        if (bitmap == null) {
            NativeLog.warn(null, "Can not process with null bitmap")
            return emptyArray()
        }
        clear(context)
        val file = cacheFile(context)
        val result = if (save(bitmap, file)) arrayOf(file.absolutePath) else {
            NativeLog.warn(null, "Can not save bitmap to ${file.absolutePath}")
            emptyArray()
        }
        bitmap.recycle()
        return result
    }

    private fun save(bitmap: Bitmap, file: File): Boolean {
        val parent = file.parentFile ?: return false
        if ((!parent.exists() && !parent.mkdirs()) || file.exists() && !file.delete()) return false
        return try {
            FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
                output.flush()
            }
            true
        } catch (error: Exception) { error.printStackTrace(); false }
    }

    fun clear(context: Context) {
        val directory = NativeDirectories.appFiles(context, "content") ?: return
        if (!directory.isDirectory) return
        directory.listFiles()?.forEach { file ->
            val name = file.name
            if (file.isFile && (name == "background.jpg" ||
                    name.startsWith("background-") && (name.endsWith(".jpeg") || name.endsWith(".jpg")))) file.delete()
        }
    }
}
