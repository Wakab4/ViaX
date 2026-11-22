package moe.via.browser.feature.download.http

import moe.via.browser.core.mime.NativeMimeTypes

import java.net.HttpURLConnection
import java.util.Locale
import moe.via.browser.feature.download.model.DownloadMetadata
import moe.via.browser.feature.download.model.DownloadTask

internal data class AcceptedDownloadProbe(val metadata: DownloadMetadata, val name: String, val mime: String)

/** m5.e.q: HTML matching precedes metadata acceptance and the optional flags4 rename. */
internal object NativeDownloadProbeMetadata {
    fun accept(connection: HttpURLConnection, task: DownloadTask): AcceptedDownloadProbe? {
        var mime = connection.contentType
        val separator = mime?.indexOf(';') ?: -1
        if (separator > 0) mime = mime?.substring(0, separator)?.trim { it <= ' ' }?.lowercase(Locale.ROOT)
        var extension = NativeMimeTypes.extension(mime)
        if (mime == null || mime == "application/octet-stream") {
            val guessed = NativeDownloadFilename.guess(connection.url.toString(), connection.getHeaderField("Content-Disposition"), null)
            val dot = guessed.lastIndexOf('.')
            if (dot >= 0) {
                extension = guessed.substring(dot + 1)
                mime = NativeMimeTypes.mime(extension, mime)
            }
        }
        val html = mime == "text/html" || extension == "html" || extension == "htm"
        if (html != (task.mime == "text/html")) return null
        var name = task.name
        var acceptedMime = task.mime
        if (task.allowMimeChange && mime != null && !mime.equals(task.mime, true)) {
            if (!extension.isNullOrEmpty() && extension != "bin") {
                val dot = name.lastIndexOf('.')
                name = if (dot >= 0) name.substring(0, dot + 1) + extension else "$name.$extension"
            }
            acceptedMime = mime
        }
        var length = if (connection.getHeaderField("Transfer-Encoding") == null)
            number(connection.getHeaderField("Content-Length")) else -1
        if (length == -1L) length = rangeLength(connection.getHeaderField("Content-Range"))
        val ranged = !html && length > 0 && ("bytes".equals(connection.getHeaderField("Accept-Ranges"), true) || connection.getHeaderField("Content-Range") != null)
        if (html) length = -1
        val etag = connection.getHeaderField("ETag")?.takeIf(String::isNotEmpty) ?: task.etag
        return AcceptedDownloadProbe(DownloadMetadata(length, ranged, etag, task.lastModified, acceptedMime), name, acceptedMime)
    }
    private fun number(value: String?) = value?.toLongOrNull() ?: -1L
    private fun rangeLength(value: String?): Long {
        if (value.isNullOrEmpty()) return -1
        val range = value.substring(6)
        val slash = range.indexOf('/')
        val dash = range.indexOf('-')
        if (slash > 0) {
            val total = range.substring(slash + 1)
            if (total != "*") return number(total)
        }
        if (!"*".startsWith(range) && dash > 0) {
            val start = number(range.substring(0, dash))
            val end = number(if (slash > 0) range.substring(dash + 1, slash) else range.substring(dash + 1))
            return (end - start + 1).takeIf { it >= 0 } ?: -1
        }
        return -1
    }
}
