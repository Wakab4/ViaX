package moe.via.browser.ui.dialog

import android.app.Dialog

/** An activity or a smaller UI owner closes every dialog it opens. */
class DialogScope {
    private val dialogs = mutableSetOf<Dialog>()
    private val effects = mutableMapOf<Dialog, AutoCloseable>()
    private var disposed = false
    val isShowing get() = dialogs.any { it.isShowing }
    fun <T : Dialog> show(dialog: T, dismissed: () -> Unit = {}): T {
        if (disposed) return dialog
        dialogs.add(dialog)
        dialog.setOnDismissListener { dialogs.remove(dialog); effects.remove(dialog)?.close(); dismissed() }
        dialog.show()
        effects.remove(dialog)?.close()
        effects[dialog] = SettingsWindowEffects.attachDialog(dialog)
        return dialog
    }
    fun dispose() {
        disposed = true; dialogs.toList().forEach(Dialog::dismiss); dialogs.clear()
        effects.values.toList().forEach(AutoCloseable::close); effects.clear()
    }
    fun updateGeometry() {
        dialogs.filter { it.isShowing }.toList().forEach { dialog ->
            if (dialog is BrowserDialog && dialog.isAnchored) dialog.dismiss()
            else if (dialog is DialogLayoutOwner) dialog.updateGeometry()
        }
    }
}
