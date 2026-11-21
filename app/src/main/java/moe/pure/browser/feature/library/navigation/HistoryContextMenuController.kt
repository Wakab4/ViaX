package moe.pure.browser.feature.library.navigation

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.view.View
import moe.pure.browser.R
import moe.pure.browser.ui.dialog.BrowserDialog

/** db.n.Z2/V2 resolves the captured adapter position when an action is selected, not the old row URL. */
internal class HistoryContextMenuController(private val activity: Activity, private val host: Host, private val notice: (String) -> Unit) : AutoCloseable {
    interface Host {
        fun url(position: Int): String?
        fun open(position: Int, where: LibraryOpenWhere)
        fun delete(position: Int)
    }
    private var dialog: BrowserDialog? = null
    private var closed = false

    fun show(anchor: View, position: Int) {
        if (closed || activity.isFinishing || activity.isDestroyed) return
        dialog?.dismiss()
        val labels = listOf(R.string.ref_library_open_background, R.string.ref_library_open_new_tab,
            R.string.ref_library_context_delete, R.string.ref_library_copy_link, R.string.ref_library_context_share)
        dialog = BrowserDialog(activity).items(labels.map(activity::getString)) { selected ->
            if (!closed && position != -1) when (selected) {
                0 -> host.open(position, LibraryOpenWhere.BACKGROUND)
                1 -> host.open(position, LibraryOpenWhere.FOREGROUND)
                2 -> host.delete(position)
                3 -> host.url(position)?.let(::copy)
                4 -> share(host.url(position))
            }
        }.anchoredTo(anchor).also { it.show() }
    }

    private fun copy(url: String) {
        activity.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, url))
        notice(activity.getString(R.string.ref_library_link_copied))
    }
    private fun share(url: String?) {
        activity.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, url)
        }, activity.getString(R.string.ref_library_context_share)))
    }
    override fun close() { closed = true; dialog?.dismiss(); dialog = null }
}
