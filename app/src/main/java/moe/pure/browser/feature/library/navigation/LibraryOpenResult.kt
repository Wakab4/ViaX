package moe.pure.browser.feature.library.navigation

import android.os.Bundle

/** l8.b preserves the original null result and a short destination array's BACKGROUND value. */
object LibraryOpenResult {
    fun read(bundle: Bundle?): List<LibraryOpenRequest>? {
        if (bundle == null || !bundle.containsKey("urls")) return null
        val urls = requireNotNull(bundle.getStringArray("urls"))
        val wheres = bundle.getIntArray("wheres")
        return urls.mapIndexedNotNull { index, url -> if (url.isEmpty()) null else
            LibraryOpenRequest(url, LibraryOpenWhere.fromNative(if (index >= wheres!!.size) 2 else wheres[index])) }
    }
    internal fun write(requests: List<LibraryOpenRequest>): Bundle? = if (requests.isEmpty()) null else Bundle().apply {
        putStringArray("urls", requests.map { it.url }.toTypedArray()); putIntArray("wheres", requests.map { it.where.nativeValue }.toIntArray())
    }
}
