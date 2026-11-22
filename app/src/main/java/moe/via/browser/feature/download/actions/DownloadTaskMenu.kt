package moe.via.browser.feature.download.actions

import android.view.View
import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference
import moe.via.browser.R
import moe.via.browser.feature.download.model.DownloadState
import moe.via.browser.feature.download.model.DownloadTask
import moe.via.browser.ui.dialog.BrowserDialog

internal enum class DownloadMenuAction { OPEN_WITH, RENAME, RESTART, COPY_LINK, QR, SHARE, DELETE }

/** b1.x4: same anchored no-title window is updated after QR decoding, rather than stacking dialogs. */
internal class DownloadTaskMenu(private val activity: FragmentActivity, private val dialogs: DownloadDialogOwner,
    private val qr: DownloadQrActions, private val selected: (DownloadMenuAction, DownloadTask) -> Unit) : AutoCloseable {
    private var popup: BrowserDialog? = null
    private var task: DownloadTask? = null
    private var anchor: WeakReference<View>? = null
    init { qr.decoded = { id -> if (task?.id == id && popup?.isShowing == true) refresh() } }
    fun show(value: DownloadTask, view: View) {
        task = value; anchor = WeakReference(view); qr.probe(value)
        val old = popup
        if (old?.isShowing == true) { refresh(); return }
        val dialog = BrowserDialog(activity).anchoredTo(view)
        popup = dialog
        configure(dialog, value)
        dialogs.show(dialog) { if (popup === dialog) { popup = null; task = null; anchor = null } }
    }
    private fun refresh() {
        val dialog = popup ?: return
        val value = task ?: return
        if (anchor?.get()?.isAttachedToWindow != true) return
        configure(dialog, value); dialog.updateGeometry()
    }
    private fun configure(dialog: BrowserDialog, value: DownloadTask) {
        val entries = mutableListOf<Pair<Int, DownloadMenuAction>>()
        if (value.state == DownloadState.COMPLETE) {
            entries += R.string.ref_download_ui_open_with to DownloadMenuAction.OPEN_WITH
            entries += R.string.ref_download_ui_rename to DownloadMenuAction.RENAME
        }
        val active = value.state in setOf(DownloadState.QUEUED, DownloadState.RUNNING, DownloadState.WAITING_NETWORK, DownloadState.PUBLISHING)
        if (!active && value.url.isNotEmpty()) entries += R.string.ref_download_ui_restart to DownloadMenuAction.RESTART
        if (value.url.isNotEmpty()) entries += R.string.ref_download_ui_copy_link to DownloadMenuAction.COPY_LINK
        if (!qr.result(value.id).isNullOrEmpty()) entries += R.string.ref_download_ui_scan to DownloadMenuAction.QR
        entries += R.string.ref_download_ui_share to DownloadMenuAction.SHARE
        entries += R.string.ref_download_ui_delete to DownloadMenuAction.DELETE
        dialog.items(entries.map { activity.getString(it.first) }) { selected(entries[it].second, value) }
    }
    override fun close() { qr.decoded = null; popup?.dismiss(); popup = null; task = null; anchor = null }
}
