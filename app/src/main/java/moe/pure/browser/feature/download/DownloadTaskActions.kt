package moe.pure.browser.feature.download

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import moe.pure.browser.feature.download.actions.DownloadDialogOwner
import moe.pure.browser.feature.download.actions.DownloadEditDialogs
import moe.pure.browser.feature.download.actions.DownloadFileSharing
import moe.pure.browser.feature.download.actions.DownloadMenuAction
import moe.pure.browser.feature.download.actions.DownloadOpenActions
import moe.pure.browser.feature.download.actions.DownloadQrActions
import moe.pure.browser.feature.download.actions.DownloadTaskMenu
import moe.pure.browser.feature.download.directory.DownloadDirectoryFragment
import moe.pure.browser.feature.download.permissions.DownloadPermissionFragment
import moe.pure.browser.feature.download.permissions.DownloadPermissionNotice
import moe.pure.browser.feature.download.workflow.DownloadContinuation

/** Coordinates task continuations; menu, forms, QR, file intents and sharing own their own work. */
class DownloadTaskActions(private val activity: FragmentActivity, private val fragments: FragmentManager = activity.supportFragmentManager,
    private val visible: () -> Boolean = { true }, removed: () -> Unit) {
    private val queue get() = DownloadCoordinator.get(activity)
    private var disposed = false
    private val dialogs = DownloadDialogOwner(activity)
    private val qr = DownloadQrActions(activity, dialogs)
    private val sharing = DownloadFileSharing(activity)
    private val opener = DownloadOpenActions(activity, fragments)
    private val edits = DownloadEditDialogs(activity, dialogs, removed) { start(it, true) }
    private val menu = DownloadTaskMenu(activity, dialogs, qr) { action, task ->
        when (action) {
            DownloadMenuAction.OPEN_WITH -> opener.open(task, true)
            DownloadMenuAction.RENAME -> edits.rename(task)
            DownloadMenuAction.RESTART -> edits.restart(task)
            DownloadMenuAction.COPY_LINK -> sharing.copy(task)
            DownloadMenuAction.QR -> qr.show(task)
            DownloadMenuAction.SHARE -> sharing.share(task)
            DownloadMenuAction.DELETE -> edits.remove(listOf(task.id))
        }
    }
    private var permissionFragment: DownloadPermissionFragment? = null
    private val deniedDelegate = lazy { DownloadPermissionNotice(activity) }
    private val denied by deniedDelegate
    private val permissionHost = object : DownloadPermissionFragment.Host {
        override fun ready() = !disposed && visible()
        override fun storageResolved(work: DownloadContinuation, allowed: Boolean) {
            if (disposed) return
            val task = work as? DownloadContinuation.Task ?: return
            if (!allowed) denied.show()
            else if (task.relocate) queue.resumeAfterDirectory(task.id, task.restart)
            else queue.requestStart(task.id, task.restart)
        }
    }
    private val directory: DownloadDirectoryDialog = DownloadDirectoryDialog(activity, DownloadDirectoryFragment.TASKS, changed = { work ->
        (work as? DownloadContinuation.Task)?.let { task ->
            if (directory.directories.tree == null && legacyStorageMissing()) requestStorage(task.copy(relocate = true))
            else queue.resumeAfterDirectory(task.id, task.restart)
        }
    }, fragments = fragments, visible = visible)
    fun toggle(task: DownloadTask) {
        when (task.state) {
            DownloadState.RUNNING, DownloadState.QUEUED, DownloadState.WAITING_NETWORK -> queue.requestPause(task.id)
            DownloadState.PUBLISHING -> Unit
            DownloadState.COMPLETE -> open(task)
            else -> start(task, false)
        }
    }
    private fun start(task: DownloadTask, restart: Boolean) {
        if (disposed || activity.isFinishing || activity.isDestroyed || fragments.isDestroyed || fragments.isStateSaved) return
        if (!directory.directories.hasAccess(Uri.parse(task.target))) directory.permissionRevoked(DownloadContinuation.Task(task.id, restart, relocate = true))
        else if (Uri.parse(task.target).scheme == "file" && legacyStorageMissing()) requestStorage(DownloadContinuation.Task(task.id, restart))
        else queue.requestStart(task.id, restart)
    }
    private fun legacyStorageMissing() = Build.VERSION.SDK_INT <= 28 &&
        activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    private fun requestStorage(work: DownloadContinuation.Task) {
        val owner = permissionFragment ?: DownloadPermissionFragment.obtain(fragments, DownloadPermissionFragment.TASKS).also {
            permissionFragment = it; it.bind(permissionHost)
        }
        owner.requestStorage(work)
    }
    fun menu(task: DownloadTask, anchor: View) { if (!disposed) menu.show(task, anchor) }
    fun remove(ids: List<Long>) { if (!disposed) edits.remove(ids) }
    fun open(task: DownloadTask, chooser: Boolean = false) { if (!disposed) opener.open(task, chooser) }
    fun onResume() {
        directory.onResume()
        DownloadPermissionFragment.existing(fragments, DownloadPermissionFragment.TASKS)?.let { permissionFragment = it; it.bind(permissionHost) }
    }
    fun dispose() {
        disposed = true; permissionFragment?.release(permissionHost); permissionFragment = null
        if (deniedDelegate.isInitialized()) denied.close()
        menu.close(); qr.close(); sharing.close(); dialogs.close(); directory.dispose()
    }
}
