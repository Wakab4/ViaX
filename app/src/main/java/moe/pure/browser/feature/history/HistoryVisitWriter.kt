package moe.pure.browser.feature.history

import android.content.Context
import moe.pure.browser.core.logging.NativeLog as Log
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.data.BrowserStore
import moe.pure.browser.feature.history.data.HistoryRows

/** z8.h.b/g7.a.c: writes survive a window and may race on a process-owned cached IO pool. */
internal object HistoryVisitWriter {
    fun submit(context: Context, url: String, title: String) {
        val app = context.applicationContext
        BrowserExecutors.io.execute {
            runCatching { BrowserStore(app).use { HistoryRows(it.writableDatabase).recordNative(url, title) } }
                .onFailure { Log.warn("History", "History worker", it) }
        }
    }
}
