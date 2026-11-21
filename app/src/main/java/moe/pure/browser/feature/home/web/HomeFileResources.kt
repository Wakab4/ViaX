package moe.pure.browser.feature.home.web

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import android.webkit.WebResourceResponse
import java.io.File
import java.util.Locale

/** File resource reads run on WebView's resource worker; image ownership is shared with image actions. */
internal object HomeFileResources {
    data class Image(val file: File, val mime: String)
    private val headers = mapOf("Cache-Control" to "no-cache", "X-Content-Type-Options" to "nosniff")

    fun image(context: Context, uri: Uri): Image? = runCatching {
        if (!HomePageLocation.local(uri)) return@runCatching null
        val path = uri.path ?: return@runCatching null
        val candidate = File(path).canonicalFile
        val owned = listOf("icon", "content").any { directory ->
            val root = File(context.filesDir, directory).canonicalFile
            candidate.path.startsWith(root.path + File.separator)
        }
        if (!owned || !candidate.isFile) return@runCatching null
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(candidate.extension.lowercase(Locale.ROOT))
            ?.takeIf { it.startsWith("image/") } ?: return@runCatching null
        Image(candidate, mime)
    }.getOrNull()

    fun response(type: String, file: File): WebResourceResponse = WebResourceResponse(type,
        if (type.startsWith("text/") || type == "image/svg+xml") "UTF-8" else null,
        200, "OK", headers, file.inputStream().buffered())

    fun missing() = WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", headers, "".byteInputStream())
}
