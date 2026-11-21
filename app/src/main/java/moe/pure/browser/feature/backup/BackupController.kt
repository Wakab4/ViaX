package moe.pure.browser.feature.backup

import android.content.Intent
import android.os.Bundle
import moe.pure.browser.feature.backup.model.BackupData
import moe.pure.browser.feature.backup.process.BackupPhase
import moe.pure.browser.feature.backup.process.BackupProcessOwner
import moe.pure.browser.feature.backup.process.BackupProcessRegistry
import moe.pure.browser.feature.backup.ui.BackupWindow
import moe.pure.browser.ui.theme.ThemedActivity

/** Binds a replaceable window to a retained app-context operation; it never owns an IO closure. */
class BackupController(private val activity: ThemedActivity, private val launchDocument: (Intent, Int) -> Unit,
    private val askPermissions: (Array<String>, Int) -> Unit, private val onImported: () -> Unit = {},
    legacyImport: ((String) -> Int)? = null) {
    private var owner = BackupProcessOwner(activity.applicationContext, legacyImport)
    private var window: BackupWindow? = null
    private var resumed = false
    private var disposed = false
    private var interrupted = false
    private val observer = BackupProcessOwner.Observer { snapshot ->
        if (!disposed && resumed && !activity.isFinishing && !activity.isDestroyed) {
            if (owner.consumeImportNotification()) onImported()
            if (!disposed && !activity.isFinishing && !activity.isDestroyed) window()?.render(snapshot)
        }
    }
    private fun window(): BackupWindow? {
        if (disposed || activity.isDestroyed || activity.isFinishing) return null
        return window ?: BackupWindow(activity, owner, launchDocument, askPermissions).also { window = it }
    }
    fun showExport() { if (!disposed) { bind(); owner.exportSelection() } }
    fun showImport() { if (!disposed) { bind(); owner.importDocument() } }
    private fun bind() { resumed = true; owner.attach(observer) }
    fun onResume() {
        if (disposed) return
        bind()
        if (interrupted) { interrupted = false; window()?.interrupted() }
    }
    fun onPause() {
        if (disposed) return
        window?.pause(activity.isChangingConfigurations); resumed = false; owner.detach(observer)
    }
    fun onActivityResult(request: Int, result: Int, data: Intent?): Boolean = window()?.result(request, result, data) ?: false
    fun onRequestPermissionsResult(request: Int, results: IntArray): Boolean = window()?.permissions(request, results) ?: false
    fun saveState(state: Bundle) {
        if (disposed) return
        window?.capture()
        state.putInt(STATE_MASK, owner.selection.fold(0) { value, item -> value or item.bit })
        if (owner.phase != BackupPhase.IDLE) state.putString(STATE_OWNER, owner.token)
    }
    fun restoreState(state: Bundle?) {
        if (disposed || state == null) return
        val restored = BackupProcessRegistry.find(state.getString(STATE_OWNER))
        if (restored != null && restored !== owner) {
            owner.detach(observer); window?.dispose(); window = null; owner.close(); owner = restored
        } else if (restored == null && state.containsKey(STATE_OWNER) && owner.phase == BackupPhase.IDLE) interrupted = true
        if (restored == null && owner.phase == BackupPhase.IDLE) {
            val mask = state.getInt(STATE_MASK, 31)
            owner.recordSelection(BackupData.entries.filterTo(linkedSetOf()) { mask and it.bit != 0 })
        }
    }
    fun dispose(retain: Boolean = activity.isChangingConfigurations) {
        if (disposed) return
        owner.detach(observer); window?.dispose(); window = null
        disposed = true; resumed = false
        if (!retain) owner.close()
    }
    companion object {
        private const val STATE_MASK = "backup_selection"
        private const val STATE_OWNER = "backup_operation_owner"
    }
}
