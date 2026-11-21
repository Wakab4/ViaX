package moe.pure.browser.feature.scripts.data

import java.io.InputStream
import java.io.InputStreamReader
import java.io.PushbackInputStream
import java.net.HttpURLConnection
import java.net.URL

object ScriptSourceLoader {
    fun load(url: String): String? {
        val source = download(url)
        if (!source.isNullOrEmpty()) return source
        return mirror(url)?.let(::download)
    }

    private fun download(url: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.readTimeout = 5000
            connection.requestMethod = "GET"
            connection.useCaches = false
            connection.connect()
            read(connection.inputStream, connection.contentEncoding)
        } catch (_: Exception) {
            try {
                connection?.errorStream?.let { read(it, null) }
            } catch (_: Exception) {
            }
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun read(input: InputStream, encoding: String?): String {
        val stream = PushbackInputStream(input, 4)
        val prefix = ByteArray(4)
        val count = stream.read(prefix, 0, prefix.size)
        val (charset, skipped) = when {
            prefix[0] == (-17).toByte() && prefix[1] == (-69).toByte() && prefix[2] == (-65).toByte() -> "UTF-8" to 3
            prefix[0] == (-2).toByte() && prefix[1] == (-1).toByte() -> "UTF-16BE" to 2
            // Via checks UTF-16LE first, including a UTF-32LE prefix.
            prefix[0] == (-1).toByte() && prefix[1] == (-2).toByte() -> "UTF-16LE" to 2
            prefix[0] == 0.toByte() && prefix[1] == 0.toByte() && prefix[2] == (-2).toByte() && prefix[3] == (-1).toByte() -> "UTF-32BE" to 4
            else -> encoding to 0
        }
        val remaining = count - skipped
        if (remaining > 0) stream.unread(prefix, skipped, remaining)
        val reader = if (charset == null) InputStreamReader(stream) else InputStreamReader(stream, charset)
        try {
            val result = StringBuilder()
            val buffer = CharArray(4096)
            while (true) {
                val size = reader.read(buffer, 0, buffer.size)
                if (size == -1) break
                if (size > 0) result.append(buffer, 0, size)
            }
            return result.toString()
        } finally {
            try { reader.close() } catch (_: Exception) { }
        }
    }

    internal fun mirror(url: String): String? {
        if (url.isEmpty() || url.startsWith("https://fastly.jsdelivr.net/")) return null
        return when {
            url.startsWith("https://cdn.jsdelivr.net/") && url.length > 25 ->
                "https://fastly.jsdelivr.net/" + url.substring(25)
            url.startsWith("https://raw.githubusercontent.com/") && url.length > 34 -> {
                val ownerEnd = url.indexOf('/', 34)
                val repositoryEnd = if (ownerEnd >= 0) url.indexOf('/', ownerEnd + 1) else -1
                if (repositoryEnd <= 0) null else
                    "https://fastly.jsdelivr.net/gh/" + url.substring(34, repositoryEnd) + '@' + url.substring(repositoryEnd + 1)
            }
            url.startsWith("https://github.com/") && url.length > 19 -> {
                val blob = url.indexOf("/blob/", 19)
                if (blob < 0 || blob >= url.length - 6) null else
                    "https://fastly.jsdelivr.net/gh/" + url.substring(19, blob) + '@' + url.substring(blob + 6)
            }
            url.startsWith("https://unpkg.com/") && url.length > 18 ->
                "https://fastly.jsdelivr.net/npm/" + url.substring(18)
            else -> null
        }
    }
}
