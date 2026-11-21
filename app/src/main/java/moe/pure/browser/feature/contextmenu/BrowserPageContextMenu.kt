package moe.pure.browser.feature.contextmenu

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.view.ContextMenu
import android.webkit.WebView
import moe.pure.browser.feature.contextmenu.image.ContextImageActions
import moe.pure.browser.feature.contextmenu.generated.HistoryContextActions
import moe.pure.browser.feature.contextmenu.generated.BookmarkContextActions
import moe.pure.browser.feature.contextmenu.generated.GeneratedLinkContextEntries
import moe.pure.browser.R
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.ui.components.BrowserMessage
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogAnchor
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.web.WebTouchPoint
import java.lang.ref.WeakReference
import java.util.Locale

class BrowserPageContextMenu(private val activity: Activity, private val host: Host) {
    interface Host {
        fun isCurrent(view: WebView): Boolean
        fun pageState(view: WebView): ContextMenuPageState
        fun open(url: String, selected: Boolean)
        fun openText(text: String)
        fun viewImage(url: String)
        fun downloadImage(view: WebView, url: String)
        fun showImageMode()
        fun markAd()
        fun customize()
        fun requestStoragePermission(onAllowed: () -> Unit)
        fun deleteHistoryLink(view: WebView, url: String)
        fun clearHistoryPage(view: WebView)
        fun editBookmarkLink(view: WebView, url: String)
        fun addBookmarkFavorite(view: WebView, url: String)
        fun deleteBookmarkLink(view: WebView, url: String)
        fun editBookmarkFolder(view: WebView, folder: String)
        fun deleteBookmarkFolder(view: WebView, folder: String)
        fun deleteSavedPage(view: WebView, url: String)
        fun sortFavorites()
        fun editFavorite(url: String)
        fun deleteFavorite(url: String)
        fun shareLink(url: String)
    }

    private val preferences = BrowserPreferences(activity.getSharedPreferences("browser", Context.MODE_PRIVATE))
    private val dialogs = DialogScope()
    private val reader = ContextNodeReader()
    private val history = HistoryContextActions(activity, dialogs)
    private val bookmarks = BookmarkContextActions(activity, dialogs, host)
    private val message = BrowserMessage(activity)
    private val images = ContextImageActions(activity, dialogs, object : ContextImageActions.Host {
        override fun open(url: String) = host.open(url, true)
        override fun openText(text: String) = host.openText(text)
        override fun requestStoragePermission(onAllowed: () -> Unit) = host.requestStoragePermission(onAllowed)
    })
    private var disposed = false
    private var current: BrowserDialog? = null

    fun downloaded(view: WebView, url: String?, data: String?): Boolean = images.downloaded(view, url, data)

    /** Generated favorite/bookmark/history entries share the same native hidden-menu pipeline. */
    fun show(view: WebView, menu: ContextMenu? = null): Boolean {
        if (disposed || !host.isCurrent(view)) return false
        val state = host.pageState(view)
        if (!state.enabled) return false
        val hit = view.hitTestResult ?: return false
        val extra = hit.extra?.takeIf { it.isNotEmpty() }
        val image = when (hit.type) {
            WebView.HitTestResult.IMAGE_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> extra
            else -> null
        }
        val link = when (hit.type) {
            WebView.HitTestResult.SRC_ANCHOR_TYPE -> extra
            WebView.HitTestResult.EMAIL_TYPE -> extra?.let { if (it.startsWith("mailto:", true)) it else "mailto:$it" }
            WebView.HitTestResult.PHONE_TYPE -> extra?.let { if (it.startsWith("tel:", true)) it else "tel:$it" }
            WebView.HitTestResult.GEO_TYPE -> extra?.let { "geo:0,0?q=" + Uri.encode(it) }
            else -> null
        }
        if (hit.type == WebView.HitTestResult.EDIT_TEXT_TYPE) return false
        val candidate = image != null || link != null || hit.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE
        if (!candidate) return false
        val reference = WeakReference(view)
        val pageUrl = view.url.orEmpty()
        val pageTitle = view.title.orEmpty()
        val point = WebTouchPoint.last(view) ?: run {
            val location = IntArray(2); view.getLocationOnScreen(location)
            DialogAnchor(location[0] + view.width / 2, location[1] + view.height / 2)
        }
        reader.read(view, link, image) { href, source, text ->
            val page = reference.get() ?: return@read
            if (disposed || !host.isCurrent(page) || page.url.orEmpty() != pageUrl) return@read
            val normalized = unwrapErrorLink(href)
            if (normalized.isNullOrEmpty() && source.isNullOrEmpty()) return@read
            val target = ContextMenuTarget(pageUrl, pageTitle, normalized, source, text, point, state)
            if (state.javaScriptEnabled && state.internalKind == -1) dispatchPageContextEvent(page, target.point)
            display(page, target, more = false)
        }
        if (candidate) menu?.clear()
        return candidate
    }

    fun dismiss() { reader.cancel(); current?.dismiss(); current = null }
    fun dispose() { if (!disposed) { disposed = true; dismiss(); bookmarks.dispose(); images.dispose(); dialogs.dispose(); message.close() } }

    private fun display(view: WebView, target: ContextMenuTarget, more: Boolean) {
        val entries = ContextMenuEntries.forTarget(target, preferences.longMenuDisabled, more)
        if (entries.isEmpty()) return
        current?.dismiss()
        val labels = entries.map { if (target.state.internalKind in setOf(1, 2, 3, 4, 5, 7, 11, 12)) GeneratedLinkContextEntries.label(activity, it) else it.label(activity) }
        current = dialogs.show(BrowserDialog(activity).items(labels) { index ->
            if (!host.isCurrent(view) || view.url.orEmpty() != target.pageUrl) return@items
            when (entries[index]) {
                PageContextAction.BACKGROUND -> target.link?.let { host.open(it, false) }
                PageContextAction.NEW_TAB -> target.link?.let { host.open(it, true) }
                PageContextAction.IMAGE -> target.image?.let(host::viewImage)
                PageContextAction.DOWNLOAD_IMAGE -> target.image?.let { host.downloadImage(view, it) }
                PageContextAction.SAVE_IMAGE -> target.image?.let { images.save(view, it) }
                PageContextAction.SHARE_IMAGE -> target.image?.let { images.share(view, it) }
                PageContextAction.SCAN_IMAGE -> target.image?.let { images.scan(view, it) }
                PageContextAction.IMAGE_SEARCH -> target.image?.let { images.reverseSearch(it, target.point) }
                PageContextAction.IMAGE_MODE -> host.showImageMode()
                PageContextAction.MARK_AD -> host.markAd()
                PageContextAction.DELETE_HISTORY -> host.deleteHistoryLink(view, target.link.orEmpty())
                PageContextAction.CLEAR_HISTORY -> history.confirmClear { host.clearHistoryPage(view) }
                PageContextAction.EDIT_BOOKMARK -> host.editBookmarkLink(view, target.link.orEmpty())
                PageContextAction.ADD_BOOKMARK_FAVORITE -> host.addBookmarkFavorite(view, target.link.orEmpty())
                PageContextAction.DELETE_BOOKMARK -> host.deleteBookmarkLink(view, target.link.orEmpty())
                PageContextAction.EDIT_BOOKMARK_FOLDER -> bookmarks.editFolder(view, target.link.orEmpty())
                PageContextAction.DELETE_BOOKMARK_FOLDER -> bookmarks.deleteFolder(view, target.link.orEmpty())
                PageContextAction.DELETE_SAVED_PAGE -> host.deleteSavedPage(view, target.link.orEmpty())
                PageContextAction.SORT_FAVORITES -> host.sortFavorites()
                PageContextAction.EDIT_FAVORITE -> host.editFavorite(target.link.orEmpty())
                PageContextAction.DELETE_FAVORITE -> host.deleteFavorite(target.link.orEmpty())
                PageContextAction.INFO -> showInfo(target)
                PageContextAction.COPY_LINK -> target.link?.let { copy(it, activity.getString(R.string.ref_context_action_link_copied)) }
                PageContextAction.COPY_TEXT -> target.linkText?.let { copy(it.trim { char -> char <= ' ' }, activity.getString(R.string.ref_context_action_text_copied)) }
                PageContextAction.SHARE_LINK -> target.link?.let(host::shareLink)
                PageContextAction.MORE -> display(view, target, more = true)
                PageContextAction.CUSTOMIZE -> host.customize()
            }
        }.anchoredAt(target.point.x, target.point.y))
    }

    private fun showInfo(target: ContextMenuTarget) {
        val body = buildList {
            if (target.pageTitle.isNotEmpty()) add("标题：\n${target.pageTitle}")
            if (target.pageUrl.isNotEmpty()) add("页面链接：\n${target.pageUrl}")
            if (!target.link.isNullOrEmpty()) add("选中链接：\n${target.link}")
        }.joinToString("\n\n")
        dialogs.show(BrowserDialog(activity, "页面信息", body).positive("确定"))
    }

    private fun copy(text: String, copied: String) {
        activity.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, text))
        message.show(copied)
    }

    private fun unwrapErrorLink(url: String?): String? {
        if (url?.startsWith("v://error/jump?url=", true) != true) return url
        return Uri.parse(url).getQueryParameter("url") ?: url
    }

    private fun dispatchPageContextEvent(view: WebView, point: DialogAnchor) {
        if (view.width <= 0 || view.height <= 0) return
        val location = IntArray(2); view.getLocationOnScreen(location)
        val x = String.format(Locale.ROOT, "%.2f", (point.x - location[0]).toFloat() / view.width)
        val y = String.format(Locale.ROOT, "%.2f", (point.y - location[1]).toFloat() / view.height)
        val script = "(function(){var a=$x,c=$y;a=Math.max(0,Math.min(1,a));c=Math.max(0,Math.min(1,c));" +
            "if(window.visualViewport){var b=window.visualViewport;a=b.offsetLeft+a*b.width;b=b.offsetTop+c*b.height}" +
            "else a*=window.innerWidth,b=c*window.innerHeight;c=document.elementFromPoint(a,b)||document;" +
            "a=new MouseEvent('contextmenu',{bubbles:!0,cancelable:!1,view:window,button:2,buttons:0,clientX:a,clientY:b});c.dispatchEvent(a)})();"
        runCatching { view.evaluateJavascript(script, null) }
    }
}
