package moe.pure.browser.feature.scripts.data

import java.io.BufferedInputStream
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** s5.b.b/c: two original GET attempts, with the second using f(url) when available. */
internal object NativeScriptResourceHttp {
    fun download(url: String, target: File): Boolean = once(url, target) || once(ScriptSourceLoader.mirror(url) ?: url, target)

    private fun once(url: String, target: File): Boolean {
        if (url.isEmpty()) return false
        val parent = target.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) return false
        var connection: HttpURLConnection? = null
        var input: BufferedInputStream? = null
        var output: FileOutputStream? = null
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.connectTimeout = 10000; connection.readTimeout = 30000
            if (connection.responseCode != 200) return false
            input = BufferedInputStream(connection.inputStream)
            output = FileOutputStream(target)
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) return true
                output.write(buffer, 0, count)
            }
        } catch (error: Exception) {
            moe.pure.browser.core.logging.NativeLog.warn("ScriptResources", "Download resource: ${error.javaClass.simpleName}")
            return false
        } finally {
            connection?.disconnect(); close(input); close(output)
        }
    }
    private fun close(value: Closeable?) { try { value?.close() } catch (_: IOException) { } }
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
}
