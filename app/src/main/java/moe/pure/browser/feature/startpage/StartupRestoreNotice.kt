package moe.pure.browser.feature.startpage

import android.app.Activity
import moe.pure.browser.R
import moe.pure.browser.ui.components.BrowserMessage
import java.lang.ref.WeakReference

/** ua.E1 snapshots PIDs before h2 closes opened records; the host owns both data operations. */
class StartupRestoreNotice(private val activity: Activity) : AutoCloseable {
    private val message = BrowserMessage(activity)
    private var disposed = false

    fun show(ids: List<String>, selected: String?, restore: (List<String>, String?) -> Unit) {
        if (disposed) return
        val captured = ids.toList()
        val owner = WeakReference(this)
        message.show(activity.getString(R.string.ref_start_restore_prompt), activity.getString(android.R.string.ok)) {
            if (owner.get()?.disposed == false) restore(captured, selected)
        }
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        message.close()
    }

    override fun close() = dispose()
}
