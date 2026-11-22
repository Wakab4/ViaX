package moe.via.browser.feature.library.history.document

import android.content.Context
import moe.via.browser.core.logging.NativeLog as Log
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.library.history.HistoryDirtyState
import moe.via.browser.feature.library.history.data.HistoryRepository
import moe.via.browser.feature.library.data.BookmarkFaviconStore

/** t9.e.f/e: submit edits then mark bit 16 immediately; document refresh does not join the SQL task. */
internal object HistoryDocumentEdits {
    fun delete(context: Context, url: String) {
        if (url.isEmpty()) return
        val app = context.applicationContext
        BrowserExecutors.io.execute {
            runCatching { HistoryRepository(app).use { it.delete(url) } }
                .onFailure { Log.warn("HistoryDocument", "Delete history link", it) }
        }
        HistoryDirtyState.markDirty()
    }

    fun clear(context: Context) {
        val app = context.applicationContext
        BrowserExecutors.io.execute {
            runCatching {
                HistoryRepository(app).use { it.clear() }
                BookmarkFaviconStore.clearHistoryAssets(app)
            }.onFailure { Log.warn("HistoryDocument", "Clear history", it) }
        }
        HistoryDirtyState.markDirty()
    }
}
