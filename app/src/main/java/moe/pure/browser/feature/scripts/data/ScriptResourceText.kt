package moe.pure.browser.feature.scripts.data

import android.util.Base64
import java.io.File
import java.io.FileReader
import java.io.FileInputStream
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.URLDecoder

/** Resource-specific s5.b.g/h/i and viewer z8.c1.O; general browser data-URI handling is unchanged. */
internal object ScriptResourceText {
    /** s5.b.j returns null for missing files and empty bytes on a read exception. */
    fun bytes(file: File?): ByteArray? {
        if (file == null || !file.exists()) return null
        try {
            val input = BufferedInputStream(FileInputStream(file))
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val count = input.read(buffer, 0, buffer.size)
                if (count == -1) break
                if (count > 0) output.write(buffer, 0, count)
            }
            input.close()
            return output.toByteArray()
        } catch (_: Exception) { return byteArrayOf() }
    }
    private fun data(url: String) = url.length > 5 && url.substring(0, 4).equals("data", true)

    fun inlineBytes(url: String): ByteArray? {
        if (!data(url)) return null
        val comma = url.indexOf(',')
        if (comma < 0) return byteArrayOf()
        return try {
            val decoded = URLDecoder.decode(url.substring(comma + 1), "UTF-8")
            if (url.lastIndexOf(";base64", comma) > 0) Base64.decode(decoded.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
            else decoded.toByteArray(Charsets.UTF_8)
        } catch (_: Exception) { byteArrayOf() }
    }

    fun inlineText(url: String): String? {
        if (!data(url)) return null
        val comma = url.indexOf(',')
        if (comma < 0) return null
        return try {
            val decoded = URLDecoder.decode(url.substring(comma + 1), "UTF-8")
            if (url.lastIndexOf(";base64", comma) > 0) Base64.decode(decoded.toByteArray(Charsets.UTF_8), Base64.DEFAULT).toString(Charsets.UTF_8)
            else decoded
        } catch (_: Exception) { null }
    }

    /** Native returns accumulated lines on a read failure; only a missing/non-file viewer target returns null. */
    fun file(file: File, trim: Boolean): String? {
        if (if (trim) !file.isFile else !file.exists()) return null
        val result = StringBuilder()
        try {
            FileReader(file).buffered().use { reader ->
                while (true) { val line = reader.readLine() ?: break; result.append(line).append('\n') }
            }
        } catch (_: Exception) { /* Preserve partial text as s5.b.i / z8.c1.O do. */ }
        return if (trim) result.toString().trim { it <= ' ' } else result.toString()
    }
}
