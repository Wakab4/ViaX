package moe.via.browser.browser.ui.contextmenu.generated

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import moe.via.browser.R
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.library.bookmarks.document.BookmarkDocumentLinks
import moe.via.browser.browser.ui.contextmenu.BrowserPageContextMenu
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import java.lang.ref.WeakReference

/** Z5 12/13: resolve folder identity before confirmation; Root owns all editing and deletion. */
internal class BookmarkContextActions(private val activity: Activity, private val dialogs: DialogScope,
    private val host: BrowserPageContextMenu.Host) {
    private var disposed = false
    private var nextRequest = 0
    private val pendingSources = mutableMapOf<Int, WebView>()
    private enum class Action { EDIT, DELETE }
    fun editFolder(view: WebView, link: String) = resolve(view, link, Action.EDIT)
    fun deleteFolder(view: WebView, link: String) = resolve(view, link, Action.DELETE)
    private fun resolved(view: WebView, key: String, action: Action) {
        if (action == Action.EDIT) { host.editBookmarkFolder(view, key); return }
        dialogs.show(BrowserDialog(activity, activity.getString(R.string.ref_generated_history_context_delete),
            activity.getString(R.string.ref_bookmark_context_delete_folder, key))
            .positive(activity.getString(R.string.ref_generated_history_context_delete)) { host.deleteBookmarkFolder(view, key); true }
            .negative(activity.getString(android.R.string.cancel)))
    }
    private fun resolve(view: WebView, link: String, action: Action) {
        if (disposed) return
        if (!link.startsWith("folder://", true)) { BookmarkDocumentLinks.linkedFolderArgument(link)?.let { resolved(view, it, action) }; return }
        val app = activity.applicationContext
        val owner = WeakReference(this)
        val request = ++nextRequest
        pendingSources[request] = view
        BrowserExecutors.persistence.execute {
            val key = runCatching { BookmarkDocumentLinks.resolveFolder(app, link) }.getOrNull()
            MAIN.post {
                val window = owner.get()?.takeUnless { it.disposed } ?: return@post
                val source = window.pendingSources.remove(request) ?: return@post
                if (key != null) window.resolved(source, key, action)
            }
        }
    }
    fun dispose() { disposed = true; pendingSources.clear() }
    private companion object { val MAIN = Handler(Looper.getMainLooper()) }
}
