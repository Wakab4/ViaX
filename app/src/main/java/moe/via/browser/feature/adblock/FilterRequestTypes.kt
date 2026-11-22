package moe.via.browser.feature.adblock

import android.webkit.MimeTypeMap
import android.webkit.WebResourceRequest
import java.util.Locale

object FilterRequestTypes {
    fun of(request: WebResourceRequest, page: String): Int {
        val url = request.url.toString()
        val main = request.isForMainFrame && url == page
        var type = if (main) 512 else 0
        if (url.startsWith("ws")) type = type or 8192
        if (request.requestHeaders["X-Requested-With"] == "XMLHttpRequest") type = type or 16384
        extension(url)?.let { return type or it }
        if (main) return type or 16
        val accept = request.requestHeaders["Accept"]
        return type or if (accept == null || accept == "*/*") 3312 else mime(accept.substringBefore(',').trim())
    }
    fun forUrl(url: String, page: String?): Int {
        var type = if (url == page) 512 else 0
        if (url.startsWith("ws")) type = type or 8192
        return type or (extension(url) ?: if (url == page) 16 else 3312)
    }
    private fun extension(url: String): Int? {
        val path = url.substringBefore('?').substringAfterLast('/')
        val extension = path.indexOf('.').takeIf { it > 0 }?.let { path.substring(it + 1).lowercase(Locale.ROOT).takeIf { value -> value.length in 1..8 } }
        when (extension) {
            "js" -> return 32
            "css" -> return 128
            "otf", "ttf", "ttc", "woff", "woff2" -> return 2048
        }
        if (extension != "php") {
            val mime = when (extension) { "json" -> "application/json"; "mht", "mhtml" -> "multipart/related"; else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.orEmpty()) }
            if (!mime.isNullOrEmpty() && mime != "application/octet-stream") return mime(mime)
        }
        return null
    }
    private fun mime(value: String): Int = when {
        value in setOf("application/javascript", "application/x-javascript", "text/javascript", "application/json") -> 32
        value == "text/css" -> 128
        value.startsWith("image/") -> 64
        value.startsWith("video/") || value.startsWith("audio/") -> 1024
        value.startsWith("font/") -> 2048
        else -> 16
    }
}
