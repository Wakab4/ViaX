package moe.pure.browser.feature.download.actions

import android.app.Dialog
import androidx.fragment.app.FragmentActivity
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.theme.ThemedActivity

/** Register with the window for geometry, but dismiss only this feature's dialogs on release. */
internal class DownloadDialogOwner(activity: FragmentActivity) : AutoCloseable {
    private val scope = (activity as? ThemedActivity)?.dialogScope ?: DialogScope()
    private val windows = mutableSetOf<Dialog>()
    private var closed = false
    val isClosed get() = closed
    fun <T : Dialog> show(dialog: T, dismissed: () -> Unit = {}): T {
        if (closed) return dialog
        windows += dialog
        return scope.show(dialog) { windows -= dialog; dismissed() }
    }
    override fun close() { closed = true; windows.toList().forEach(Dialog::dismiss); windows.clear() }
}
