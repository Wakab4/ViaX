package moe.pure.browser.feature.home.icons

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.util.Locale

/** w3.m/r/n keep homepage icons separate from the favicon directory. */
internal object NativeHomeIconPaths {
    fun directory(context: Context) = File(context.filesDir, "icon")
    fun file(context: Context, url: String) = File(directory(context), key(url) + ".png")
    fun relative(url: String) = ".${File.separator}icon${File.separator}${key(url)}.png"

    fun key(url: String): String {
        if (url.isEmpty()) return "untitled"
        if (url.length > 6 && (url.startsWith("http://", true) || url.startsWith("https://", true))) {
            val start = url.indexOf("://") + 3
            var end = url.indexOf('/', start)
            if (end < 0) end = url.indexOf('?', start)
            if (end < 0) end = url.length
            if (end > start) return url.substring(start, end).lowercase(Locale.ROOT)
        }
        val value = url.lowercase(Locale.ROOT)
        return try {
            val bytes = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
            buildString(bytes.size * 2) {
                for (byte in bytes) {
                    val number = byte.toInt() and 255
                    if (number < 16) append('0')
                    append(Integer.toHexString(number))
                }
            }
        } catch (error: Exception) { error.printStackTrace(); value }
    }
}
