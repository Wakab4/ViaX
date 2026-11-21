package moe.pure.browser.feature.library.popup

import moe.pure.browser.R
import moe.pure.browser.ui.components.BrowserMessage
import moe.pure.browser.ui.theme.ThemedActivity

/** Browser V7/l8 owns b8.n; RecordContainer forms have their separate Activity facade. */
class BookmarkPopupController(private val activity: ThemedActivity, private val ready: () -> Boolean,
    private val changed: () -> Unit) : BookmarkPopupDialogFragment.Host {
    private val messages = BrowserMessage(activity)
    private var closed = false

    fun show(url: String? = null, title: String? = null, id: String? = null, position: Int = -1) {
        if (closed || !ready() || activity.isFinishing || activity.isDestroyed) return
        val manager = activity.supportFragmentManager
        if (manager.isStateSaved || manager.findFragmentByTag(BookmarkPopupDialogFragment.TAG) != null) return
        BookmarkPopupDialogFragment.create(BookmarkPopupRequest(id, url, title, position)).also {
            it.bind(this)
            it.show(manager, BookmarkPopupDialogFragment.TAG)
        }
    }

    fun rebind() {
        if (!closed && ready()) restored(activity)?.bind(this)
    }

    override fun completed(event: BookmarkPopupEvent) {
        if (closed) return
        when (event.kind) {
            BookmarkPopupEventKind.SAVED -> { messages.show(activity.getString(R.string.ref_bookmark_popup_added)); changed() }
            BookmarkPopupEventKind.DELETED -> {
                messages.show(activity.getString(R.string.ref_bookmark_popup_deleted, event.entry?.title.orEmpty())); changed()
            }
            else -> Unit
        }
    }

    fun close() {
        closed = true
        restored(activity)?.release(this)
        messages.close()
    }

    companion object {
        private fun restored(activity: ThemedActivity) = activity.supportFragmentManager
            .findFragmentByTag(BookmarkPopupDialogFragment.TAG) as? BookmarkPopupDialogFragment
        fun hasRestored(activity: ThemedActivity) = restored(activity) != null
    }
}
