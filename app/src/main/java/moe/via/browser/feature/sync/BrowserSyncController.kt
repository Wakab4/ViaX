package moe.via.browser.feature.sync

import android.content.Context
import moe.via.browser.feature.sync.sync.SyncManager

/** Browser resume is the only automatic trigger; the manager owns configuration and its successful-run throttle. */
class BrowserSyncController(context: Context, private val imported: () -> Unit) {
    private val manager = SyncManager(context.applicationContext)
    private var disposed = false
    fun resume() {
        if (disposed) return
        manager.onBrowserResume { result ->
            if (disposed || result.skipped || result.cancelled) return@onBrowserResume
            if (result.success) imported()
            else android.util.Log.w("BrowserSync", "自动同步失败：" + (result.error ?: result.errors.joinToString { it.error.orEmpty() }))
        }
    }
    fun dispose() { disposed = true; manager.close() }
}
