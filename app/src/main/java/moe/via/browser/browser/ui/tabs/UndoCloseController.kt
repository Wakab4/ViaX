package moe.via.browser.browser.ui.tabs

import android.app.Activity
import moe.via.browser.R
import moe.via.browser.ui.message.MessageContent
import moe.via.browser.ui.message.MessageOwner
import moe.via.browser.webview.compat.NativePageAddress
import moe.via.browser.browser.session.ClosedTabBatch
import moe.via.browser.browser.session.TabSnapshot

/** ua.R/f0 use the same process message slot as other browser notices. */
class UndoCloseController(private val activity: Activity) {
    private val messages = MessageOwner(activity)

    fun show(batch: ClosedTabBatch, onUndo: () -> Unit) {
        val first = batch.tabs.firstOrNull() ?: return
        val caption = caption(first)
        val text = if (batch.tabs.size == 1 && (batch.closedCount == 1 || caption.isNotEmpty()))
            activity.getString(R.string.ref_tab_undo_closed_title, caption)
        else activity.resources.getQuantityString(R.plurals.ref_tab_undo_closed_count, batch.closedCount, batch.closedCount)
        messages.show(MessageContent(text, activity.getString(R.string.ref_tab_undo_action), clicked = onUndo))
    }

    private fun caption(tab: TabSnapshot): String {
        if (tab.title.isNotEmpty()) return tab.title.take(128)
        if (tab.url.isEmpty()) return activity.getString(R.string.ref_tab_undo_untitled)
        val host = NativePageAddress.authority(tab.url)
        val dot = host.indexOf('.')
        val start = when {
            dot == 1 && host[0] == 'm' && host.indexOf('.', 2) >= 0 -> 2
            dot == 3 && host.startsWith("www") && host.indexOf('.', 4) >= 0 -> 4
            else -> 0
        }
        return host.substring(start).take(128)
    }

    fun close() = messages.close()
}
