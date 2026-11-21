package moe.pure.browser.feature.download

import android.webkit.CookieManager
import java.io.Closeable
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import moe.pure.browser.feature.download.http.NativeDownloadConnection
import moe.pure.browser.feature.download.http.NativeDownloadHeaders
import moe.pure.browser.feature.download.transfer.DownloadFailure

/** Confirmation HEAD and task GET retain their distinct Native owners and connection policies. */
internal class DownloadHttp(private val control: DownloadControl) {
    fun head(url: String, headers: Map<String, String>): DownloadHead? {
        val connection = URL(url).openConnection() as HttpURLConnection
        control.attach(connection)
        try {
            connection.requestMethod = "HEAD"
            CookieManager.getInstance().getCookie(url)?.let { connection.setRequestProperty("Cookie", it) }
            connection.setRequestProperty("Referer", url)
            headers.entries.firstOrNull { it.key.equals("User-Agent", true) }?.let { connection.setRequestProperty("User-Agent", it.value) }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            return DownloadHead(connection.contentLengthLong, connection.contentType?.substringBefore(';')?.trim(), connection.getHeaderField("Content-Disposition"))
        } finally { control.detach(connection) }
    }
    fun open(task: DownloadTask, part: DownloadPart, received: Long): Response {
        val connection = NativeDownloadConnection(control).open(task.url, { NativeDownloadHeaders.part(it, task, part, received) })
        try {
            val code = connection.responseCode
            if (code != 200 && code != 206) throw DownloadFailure.http(code, connection.responseMessage)
            if (code == 200 && received > 0) throw DownloadFailure(24)
            val length = connection.getHeaderField("Content-Length")?.toLongOrNull()
            if (part.length > 0 && length != null && length > part.length) throw DownloadFailure(24)
            val needsLength = part.length <= 0 && !"close".equals(connection.getHeaderField("Connection"), true) &&
                !"chunked".equals(connection.getHeaderField("Transfer-Encoding"), true)
            if (needsLength && (length == null || length <= 0 || part.start != 0L)) throw DownloadFailure(24)
            return Response(connection, connection.inputStream, if (needsLength) length!! else part.length)
        } catch (error: Exception) { control.detach(connection); throw error }
    }
    inner class Response(private val connection: HttpURLConnection, val input: InputStream, val length: Long) : Closeable {
        override fun close() { try { input.close() } finally { control.detach(connection) } }
    }
}
