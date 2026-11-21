package moe.pure.browser.feature.adblock.update

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** b5.c.d/e writes directly and only retries when its original mirror mapper has an address. */
internal object NativeFilterDownload {
    fun download(url: String, path: String?): Boolean {
        if (once(url, path)) return true
        return mirror(url)?.let { once(it, path) } ?: false
    }
    private fun once(url: String, path: String?): Boolean {
        if (url.isEmpty() || path.isNullOrEmpty()) return false
        var input: BufferedInputStream? = null
        var output: BufferedOutputStream? = null
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"; connection.doOutput = false; connection.doInput = true
            connection.connectTimeout = 5000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connect()
            val status = connection.responseCode
            val type = connection.getHeaderField("Content-Type")
            if (status != 200 || type == null || !type.trim { it <= ' ' }.startsWith("text/plain")) return false
            input = BufferedInputStream(connection.inputStream)
            output = BufferedOutputStream(FileOutputStream(path))
            val buffer = ByteArray(1024)
            while (true) { val count = input.read(buffer); if (count == -1) break; output.write(buffer, 0, count) }
            true
        } catch (_: Exception) { false }
        finally { close(input); close(output) }
    }
    private fun close(value: Closeable?) { try { value?.close() } catch (_: IOException) { } }
    private fun mirror(url: String): String? {
        if (url.isEmpty() || url.startsWith("https://fastly.jsdelivr.net/")) return null
        return when {
            url.startsWith("https://cdn.jsdelivr.net/") && url.length > 25 -> "https://fastly.jsdelivr.net/" + url.substring(25)
            url.startsWith("https://raw.githubusercontent.com/") && url.length > 34 -> {
                val first = url.indexOf('/', 34)
                val second = if (first < 0) -1 else url.indexOf('/', first + 1)
                if (second <= 0) null else "https://fastly.jsdelivr.net/gh/" + url.substring(34, second) + '@' + url.substring(second + 1)
            }
            url.startsWith("https://github.com/") && url.length > 19 -> "https://fastly.jsdelivr.net/gh/" + url.substring(19).replace("/blob/", "@")
            url.startsWith("https://unpkg.com/") && url.length > 18 -> "https://fastly.jsdelivr.net/npm/" + url.substring(18)
            else -> null
        }
    }
    private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36"
}
