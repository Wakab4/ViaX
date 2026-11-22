package moe.via.browser.feature.library.bookmarks.document

import android.content.Context
import moe.via.browser.feature.library.data.LibraryRepository
import java.net.URLDecoder

/** z8.w2.d/g/i plus Z5's folder:// title lookup, without changing stored folder identities. */
object BookmarkDocumentLinks {
    fun folder(key: String?): String = if (key.isNullOrEmpty()) "v://bookmarks" else "v://bookmarks/?folder=$key"
    fun bookmark(url: String): String = if (url.startsWith("http://", true) || url.startsWith("https://", true) ||
        url.startsWith("file://", true) || url.startsWith("data:", true))
        "v://error/jump?url=" + java.net.URLEncoder.encode(url, "UTF-8").replace("+", "%20") else url

    fun isFolderLink(url: String): Boolean = url.startsWith("v://bookmarks", true) &&
        (url.length == 13 || url.getOrNull(13) == '?' || url.getOrNull(13) == '/')

    /** First literal ?folder=, raw value through the first &: no URI decoding. */
    fun linkedFolderArgument(url: String): String? {
        if (!isFolderLink(url)) return null
        val marker = url.indexOf("?folder=")
        if (marker < 0) return null
        val start = marker + 8
        val end = url.indexOf('&', start).takeIf { it >= 0 } ?: url.length
        return url.substring(start, end)
    }

    /** Worker only when folder:// requires the repository title lookup. */
    fun resolveFolder(context: Context, link: String): String? {
        if (link.startsWith("folder://", true)) {
            val title = URLDecoder.decode(link.substring(9), "UTF-8")
            return LibraryRepository(context.applicationContext).use { it.findFolderByTitle(title)?.id?.let(::key) }
        }
        return linkedFolderArgument(link)
    }
    internal fun key(id: String) = id
}
