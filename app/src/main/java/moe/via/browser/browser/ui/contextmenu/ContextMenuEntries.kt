package moe.via.browser.browser.ui.contextmenu

import android.webkit.URLUtil
import moe.via.browser.browser.ui.contextmenu.generated.HistoryContextEntries
import moe.via.browser.browser.ui.contextmenu.generated.BookmarkContextEntries
import moe.via.browser.browser.ui.contextmenu.generated.GeneratedLinkContextEntries
import moe.via.browser.browser.ui.contextmenu.generated.FavoriteContextEntries

/** GP s6.o9 ordinary-page branch, with generated history delegated to its own entries. */
internal object ContextMenuEntries {
    fun forTarget(target: ContextMenuTarget, hidden: Set<Int>, more: Boolean): List<PageContextAction> {
        if (target.state.internalKind == 1) return FavoriteContextEntries.entries(target, hidden, more)
        if (target.state.internalKind == 3) return HistoryContextEntries.entries(target, hidden, more)
        if (target.state.internalKind in setOf(2, 11)) return BookmarkContextEntries.entries(target, hidden, more)
        if (target.state.internalKind in setOf(4, 5, 12)) return GeneratedLinkContextEntries.entries(target, hidden, more)
        if (target.state.internalKind == 7) return GeneratedLinkContextEntries.entries(target, hidden, more, listOf(PageContextAction.DELETE_SAVED_PAGE), 1)
        val entries = buildList {
            val link = target.link.orEmpty()
            val image = target.image.orEmpty()
            val networkPage = URLUtil.isNetworkUrl(target.pageUrl)
            val shareableImage = image.isNotEmpty() && (image.startsWith("http://", true) || image.startsWith("https://", true) ||
                image.startsWith("data:", true) || image.startsWith("file://", true))
            if (!target.state.customTab && URLUtil.isNetworkUrl(link)) {
                add(PageContextAction.BACKGROUND); add(PageContextAction.NEW_TAB)
            }
            if (image.isNotEmpty()) {
                add(PageContextAction.IMAGE); add(PageContextAction.DOWNLOAD_IMAGE); add(PageContextAction.SAVE_IMAGE)
                if (shareableImage) add(PageContextAction.SHARE_IMAGE)
                if (URLUtil.isNetworkUrl(image)) add(PageContextAction.IMAGE_SEARCH)
                if (networkPage && target.state.javaScriptEnabled) add(PageContextAction.IMAGE_MODE)
            }
            add(PageContextAction.INFO)
            if (networkPage && target.state.javaScriptEnabled && target.state.adBlockingEnabled) add(PageContextAction.MARK_AD)
            if (!target.linkText.isNullOrEmpty()) add(PageContextAction.COPY_TEXT)
            if (shareableImage) add(PageContextAction.SCAN_IMAGE)
            if (link.isNotEmpty()) { add(PageContextAction.COPY_LINK); add(PageContextAction.SHARE_LINK) }
        }
        val hiddenEntries = entries.filter { it.nativeId in hidden }
        return if (more) {
            if (hiddenEntries.isEmpty()) emptyList() else hiddenEntries + PageContextAction.CUSTOMIZE
        } else {
            val visible = entries.filter { it.nativeId !in hidden }
            visible + if (hiddenEntries.isNotEmpty()) PageContextAction.MORE else PageContextAction.CUSTOMIZE
        }
    }
}
