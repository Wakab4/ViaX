package moe.pure.browser.data

import android.content.Context
import android.os.Handler
import android.os.Looper

/** Opens/upgrades the shared database before the main browser constructs feature repositories. */
class BrowserDatabaseStartup(context: Context) {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var opening = false
    private var disposed = false
    var ready = false
        private set

    fun prepare(completed: () -> Unit, failed: (Throwable) -> Unit) {
        if (disposed || opening) return
        if (ready) { completed(); return }
        opening = true
        BrowserExecutors.persistence.execute {
            val result = runCatching {
                BrowserStore(app).use { moe.pure.browser.feature.site.SiteSettingsRepository.preload(it.writableDatabase) }
            }
            main.post {
                if (!disposed) {
                    opening = false
                    result.onSuccess { ready = true; completed() }.onFailure(failed)
                }
            }
        }
    }

    fun dispose() { disposed = true; main.removeCallbacksAndMessages(null) }
}
