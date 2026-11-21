package moe.pure.browser.feature.scripts.runtime.http

import android.util.Base64
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.util.Locale

/** o5.b reads 6144-byte binary or 4096-character text chunks, with its original 8 MiB gate. */
internal object NativeGmHttpBody {
    private const val LIMIT = 8388608
    class Result(val value: String, val binary: Boolean, val tooLarge: Boolean)

    fun read(connection: HttpURLConnection, type: String, response: NativeGmHttpResponse, contentLength: Int,
        progress: () -> Unit): Result {
        val binary = type == "blob" || type == "stream" || type == "arraybuffer"
        return if (binary) binary(connection, response, contentLength, progress) else text(connection, response, progress)
    }

    private fun binary(connection: HttpURLConnection, response: NativeGmHttpResponse, contentLength: Int, progress: () -> Unit): Result {
        val input = BufferedInputStream(connection.inputStream)
        var mime = connection.contentType
        mime?.indexOf(';')?.takeIf { it > 0 }?.let { mime = mime!!.substring(0, it) }
        val value = StringBuilder("data:${mime ?: "text/plain"};base64,")
        val buffer = ByteArray(6144)
        var carry = byteArrayOf()
        var pending: String? = null
        var loaded = 0
        var previous = 0L
        var tooLarge = false
        while (true) {
            val count = input.read(buffer, 0, buffer.size)
            if (count <= 0) break
            if (loaded >= LIMIT) { tooLarge = true; break }
            val encoded = if (carry.isEmpty()) Base64.encodeToString(buffer, 0, count, Base64.NO_WRAP) else {
                carry = carry.copyOf(carry.size + count)
                System.arraycopy(buffer, 0, carry, carry.size - count, count)
                Base64.encodeToString(carry, 0, carry.size, Base64.NO_WRAP)
            }
            if ('=' !in encoded) {
                if (carry.isNotEmpty()) carry = byteArrayOf()
                value.append(encoded); pending = null
            } else {
                if (carry.isEmpty()) carry = buffer.copyOf(count)
                pending = encoded
            }
            loaded += count
            if (loaded > 0 && contentLength > 0) {
                response.loaded = loaded.toLong()
                if (loaded.toLong() - previous > 32768) progress()
                previous = loaded.toLong()
            }
        }
        if (pending != null) value.append(pending)
        input.close()
        return Result(if (tooLarge) "" else value.toString(), true, tooLarge)
    }

    private fun text(connection: HttpURLConnection, response: NativeGmHttpResponse, progress: () -> Unit): Result {
        var encoding = connection.contentEncoding
        if (encoding == null) {
            val contentType = connection.contentType
            if (!contentType.isNullOrEmpty()) {
                val upper = contentType.uppercase(Locale.ROOT)
                val marker = upper.indexOf("CHARSET=")
                if (marker >= 0) {
                    val start = marker + 8
                    val end = upper.indexOf(';', start).takeIf { it >= 0 } ?: upper.length
                    if (end > start) encoding = upper.substring(start, end)
                }
            }
        }
        val input = NativeGmTextReader(connection.inputStream, encoding)
        val value = StringBuilder()
        val buffer = CharArray(4096)
        var loaded = 0
        var previous = 0L
        var tooLarge = false
        while (true) {
            val count = input.read(buffer, 0, buffer.size)
            if (count <= 0) break
            if (loaded >= LIMIT) { tooLarge = true; break }
            value.append(buffer, 0, count); loaded += count
            if (loaded > 0) {
                response.loaded = loaded.toLong()
                if (loaded.toLong() - previous > 32768) progress()
                previous = loaded.toLong()
            }
        }
        input.close()
        return Result(if (tooLarge) "" else value.toString(), false, tooLarge)
    }
}
