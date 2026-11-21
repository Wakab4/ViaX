package moe.pure.browser.core

import android.content.Context
import moe.pure.browser.feature.home.HomeDocument

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
}
