package moe.pure.browser.feature.history.document

import android.content.Context
import moe.pure.browser.core.logging.NativeLog as Log
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.data.BrowserStore
import moe.pure.browser.feature.history.HistoryDirtyState
import moe.pure.browser.feature.history.data.HistoryRows
import moe.pure.browser.feature.library.data.BookmarkFaviconStore

/** t9.e.f/e: submit edits then mark bit 16 immediately; document refresh does not join the SQL task. */
internal object HistoryDocumentEdits {
    fun delete(context: Context, url: String) {
        if (url.isEmpty()) return
        val app = context.applicationContext
        BrowserExecutors.io.execute {
            runCatching { BrowserStore(app).use { HistoryRows(it.writableDatabase).deleteUrl(url) } }
                .onFailure { Log.warn("HistoryDocument", "Delete history link", it) }
        }
        HistoryDirtyState.markDirty()
    }

    fun clear(context: Context) {
        val app = context.applicationContext
        BrowserExecutors.io.execute {
            runCatching {
                BrowserStore(app).use { HistoryRows(it.writableDatabase).clear() }
                BookmarkFaviconStore.clearHistoryAssets(app)
            }.onFailure { Log.warn("HistoryDocument", "Clear history", it) }
        }
        HistoryDirtyState.markDirty()
    }
}
