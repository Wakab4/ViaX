package moe.via.browser.feature.internalpages

import android.content.Context
import moe.via.browser.R
import moe.via.browser.feature.home.HomeDocument

/** Native u9.d.d/m: generated documents only; arbitrary file/about pages remain ordinary pages. */
internal object BrowserInternalPages {
    private val kinds = mapOf("homepage.html" to 1, "homepage2.html" to 1, "bookmarks.html" to 2,
        "folder.html" to 11, "history.html" to 3, "catalog.html" to 5, "log.html" to 6,
        "res.html" to 10, "save.html" to 7, "about.html" to 4, "blank.html" to 12, "images.html" to 13)

    /** u9.d.d reads the raw generated file prefix, without URI decoding/canonicalizing it. */
    fun kind(context: Context, url: String?): Int {
        if (url.isNullOrEmpty()) return 0
        val prefix = "file://${context.filesDir.path}/"
        if (!url.startsWith(prefix)) return -1
        var name = url.substring(prefix.length)
        name.indexOf('?').takeIf { it > 0 }?.let { name = name.substring(0, it) }
        name.indexOf('#').takeIf { it > 0 }?.let { name = name.substring(0, it) }
        return kinds[name] ?: -1
    }

    fun isInternal(context: Context, url: String): Boolean {
        return kind(context, url) > 0
    }

    fun tabTitle(context: Context, url: String, documentTitle: String = ""): String? {
        val pageKind = when {
            url.startsWith("file:") -> kind(context, url)
            url.startsWith("about:", ignoreCase = true) && HomeDocument.isHome(context, url) -> 1
            else -> return null
        }
        if (pageKind == 11 && documentTitle.isNotBlank() &&
            !documentTitle.startsWith("file:", ignoreCase = true) && documentTitle != "folder.html") return documentTitle
        val resource = when (pageKind) {
            1 -> R.string.ref_home_page_title
            2, 11 -> R.string.ref_bookmark_page_title
            3 -> R.string.ref_history_page_title
            4 -> R.string.ref_about_document_title
            5 -> R.string.ref_catalog_page_title
            6 -> R.string.ref_network_page_log_title
            7 -> R.string.ref_saved_page_title
            10 -> R.string.ref_network_page_resource_title
            12 -> R.string.ref_blank_page_title
            13 -> R.string.ref_image_page_title
            else -> return null
        }
        return context.getString(resource)
    }
}
