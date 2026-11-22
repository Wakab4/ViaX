package moe.via.browser.feature.library.history

import android.content.Context
import moe.via.browser.core.logging.NativeLog as Log
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.library.history.data.HistoryRepository

/** z8.h.b/g7.a.c: writes survive a window and may race on a process-owned cached IO pool. */
internal object HistoryVisitWriter {
    fun submit(context: Context, url: String, title: String) {
        val app = context.applicationContext
        BrowserExecutors.io.execute {
            runCatching { HistoryRepository(app).use { it.recordVisit(url, title) } }
                .onFailure { Log.warn("History", "History worker", it) }
        }
    }
}
