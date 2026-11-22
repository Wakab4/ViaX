package moe.via.browser.feature.library.history.data

import android.content.Context
import moe.via.browser.storage.database.BrowserDatabase

internal class HistoryRepository(context: Context) : AutoCloseable {
    private val database = BrowserDatabase(context.applicationContext)

    fun recent(limit: Int): List<HistoryRecord> =
        HistoryRows(database.readableDatabase).read(limit = limit.toString()).rows

    fun recordVisit(url: String, title: String) {
        HistoryRows(database.writableDatabase).recordNative(url, title)
    }

    fun delete(url: String) {
        HistoryRows(database.writableDatabase).deleteUrl(url)
    }

    fun clear() {
        HistoryRows(database.writableDatabase).clear()
    }

    override fun close() = database.close()
}
