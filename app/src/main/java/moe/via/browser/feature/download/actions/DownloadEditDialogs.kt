package moe.via.browser.feature.download.actions

import androidx.fragment.app.FragmentActivity
import java.lang.ref.WeakReference
import moe.via.browser.R
import moe.via.browser.feature.download.transfer.DownloadCoordinator
import moe.via.browser.feature.download.model.DownloadState
import moe.via.browser.feature.download.model.DownloadTask
import moe.via.browser.ui.dialog.BrowserDialog

/** b1.m4/n4/o4/s4 forms. Queue and physical IO are not owned by the dialog. */
internal class DownloadEditDialogs(private val activity: FragmentActivity, private val owner: DownloadDialogOwner,
    private val removed: () -> Unit, private val restart: (DownloadTask) -> Unit) {
    private val queue get() = DownloadCoordinator.get(activity)

    fun rename(task: DownloadTask) {
        if (task.state != DownloadState.COMPLETE) return
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_download_ui_rename))
        val field = dialog.field(task.name, task.name)
        field.setSelectAllOnFocus(true)
        field.setOnFocusChangeListener { _, focused -> if (focused) {
            val name = field.text.toString(); val dot = name.lastIndexOf('.')
            if (dot < 0 || name.length - dot >= 7) field.selectAll() else field.setSelection(0, dot)
        } }
        owner.show(dialog.positive(activity.getString(android.R.string.ok)) {
            val value = field.text.toString()
            if (value.isNotEmpty() && !task.name.equals(value, true)) queue.rename(task.id, value) { result ->
                result.exceptionOrNull()?.let { moe.via.browser.core.logging.NativeLog.warn("DownloadRename", "Rename failed", it) }
            }
            true
        }.negative(activity.getString(android.R.string.cancel)))
    }
    fun restart(task: DownloadTask) {
        owner.show(BrowserDialog(activity, activity.getString(R.string.ref_download_ui_restart),
            activity.getString(R.string.ref_download_action_restart_body, task.name))
            .positive(activity.getString(android.R.string.ok)) { restart.invoke(task); true }
            .negative(activity.getString(android.R.string.cancel)))
    }
    fun remove(ids: List<Long>) {
        val tasks = queue.snapshot.filter { it.id in ids }
        if (tasks.isEmpty()) return
        val bulk = ids.size > 1
        val complete = tasks.singleOrNull()?.state == DownloadState.COMPLETE
        val body = if (bulk) activity.getString(R.string.ref_download_action_delete_bulk, ids.size)
            else activity.getString(if (complete) R.string.ref_download_action_delete_complete else R.string.ref_download_action_delete_partial)
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_download_ui_delete), body)
        val check = if (bulk || complete) dialog.checkbox(activity.getString(R.string.ref_download_action_delete_files), false) else null
        owner.show(dialog.positive(activity.getString(android.R.string.ok)) {
            val reference = WeakReference(this)
            val completed: (Result<Unit>) -> Unit = { result ->
                result.exceptionOrNull()?.let { moe.via.browser.core.logging.NativeLog.warn("DownloadDelete", "Delete failed", it) }
                if (result.isSuccess) reference.get()?.let { current ->
                    if (!current.owner.isClosed && !current.activity.isDestroyed) current.removed()
                }
            }
            if (bulk) queue.removeSelection(ids, check?.isChecked == true, completed)
            else queue.remove(ids, !complete || check?.isChecked == true, completed)
            true
        }.negative(activity.getString(android.R.string.cancel)))
    }
}
