package moe.via.browser.feature.download.http

import java.net.HttpURLConnection
import moe.via.browser.feature.download.model.DownloadPart
import moe.via.browser.feature.download.model.DownloadTask

/** m5.e.f and m5.d.e apply different headers for metadata negotiation and chunk IO. */
internal object NativeDownloadHeaders {
    fun probe(connection: HttpURLConnection, task: DownloadTask, referer: String?, userAgent: String?) {
        task.headers.forEach { (key, value) ->
            if (!key.equals("Referer", true) && !key.equals("User-Agent", true)) connection.addRequestProperty(key, value)
        }
        referer?.let { connection.setRequestProperty("Referer", it) }
        userAgent?.let { connection.setRequestProperty("User-Agent", it) }
    }
    fun part(connection: HttpURLConnection, task: DownloadTask, part: DownloadPart, received: Long) {
        var etag = task.etag
        task.headers.forEach { (key, value) ->
            if (key.equals("ETag", true)) etag = value
            else if (key.equals("Referer", true)) connection.setRequestProperty("Referer", value)
            else connection.addRequestProperty(key, value)
        }
        connection.setRequestProperty("Accept-Encoding", "identity")
        connection.setRequestProperty("Connection", "close")
        if (received > 0 && etag != null) connection.addRequestProperty("If-Match", etag)
        if (part.length > 0) connection.addRequestProperty("Range", "bytes=${part.start + received}-${part.end}")
    }
}
