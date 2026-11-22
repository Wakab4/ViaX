package moe.via.browser.feature.library.history.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import moe.via.browser.core.logging.NativeLog as Log
import java.util.Calendar

/** ga.b SQL operates on seconds and physical row IDs; no URL UNIQUE constraint or cache is added. */
class HistoryRows(private val db: SQLiteDatabase) {
    fun read(selection: String? = null, arguments: Array<String>? = null, limit: String? = null): HistoryRowsRead {
        val rows = mutableListOf<HistoryRecord>()
        return try {
            db.query("history", COLUMNS, selection, arguments, null, null, "updated_at DESC", limit).use { cursor ->
                while (cursor.moveToNext()) rows.add(HistoryRecord(cursor.getLong(0), cursor.getString(1), cursor.getString(2),
                    cursor.getLong(3)))
            }
            HistoryRowsRead(rows)
        } catch (error: Exception) {
            Log.warn("History", "History query", error)
            HistoryRowsRead(rows, error)
        }
    }

    /** Called on the writer thread: ga.b.t samples wall clock inside the delete/insert transaction. */
    fun recordNative(url: String, title: String): Long {
        var id = -1L
        try {
            transaction {
                deleteUrl(url)
                id = db.insert("history", null, ContentValues().apply {
                    put("url", url); put("title", title)
                    put("updated_at", System.currentTimeMillis() / 1000)
                })
            }
        } catch (error: Exception) { Log.warn("History", "History write", error) }
        return id
    }

    /** ga.b.x does not compare timestamps. Only zero uses its per-batch moving fallback clock. */
    fun importNative(records: List<HistoryRecord>): Int {
        if (records.isEmpty()) return 0
        var count = 0
        try {
            transaction {
                var fallback = System.currentTimeMillis() / 1000 - records.size
                for (record in records) {
                    val url = record.url.orEmpty()
                    deleteUrl(url)
                    val row = ContentValues().apply {
                        put("url", url); put("title", record.title.orEmpty())
                        put("updated_at", if (record.updatedAtSeconds == 0L) fallback else record.updatedAtSeconds)
                    }
                    fallback++
                    if (db.insert("history", null, row) > 0) count++
                }
            }
        } catch (error: Exception) { Log.warn("History", "History import", error) }
        return count
    }

    /** ga.b.i: first the last-hour count, then unsorted raw-timezone day groups. */
    fun periodCounts(): List<HistoryTimeCount> {
        val counts = mutableListOf<HistoryTimeCount>()
        val rawOffset = Calendar.getInstance().timeZone.rawOffset / 1000
        val hour = System.currentTimeMillis() / 1000 - 3600
        try {
            db.rawQuery("SELECT count(id) FROM history WHERE updated_at >= ?", arrayOf(hour.toString())).use { cursor ->
                while (cursor.moveToNext()) counts.add(HistoryTimeCount(hour, cursor.getInt(0)))
            }
        } catch (error: Exception) { Log.warn("History", "History hour count", error) }
        try {
            db.rawQuery("SELECT (updated_at+$rawOffset)/86400 AS range_id,count(id) FROM history GROUP BY range_id", null).use { cursor ->
                while (cursor.moveToNext()) counts.add(HistoryTimeCount(cursor.getLong(0) * 86400 - rawOffset, cursor.getInt(1)))
            }
        } catch (error: Exception) { Log.warn("History", "History day count", error) }
        return counts
    }

    fun deleteIds(ids: LongArray): Int = if (ids.isEmpty()) 0 else db.delete("history",
        "id IN (${ids.joinToString(",") { "?" }})", ids.map(Long::toString).toTypedArray())
    fun deleteUrl(url: String): Int = db.delete("history", "url = ?", arrayOf(url))
    fun clearSince(sinceSeconds: Long): Int = db.delete("history", "updated_at >= ?", arrayOf(sinceSeconds.toString()))
    fun clear(): Int = db.delete("history", null, null)

    private fun <T> transaction(block: () -> T): T {
        if (db.inTransaction()) return block()
        db.beginTransaction()
        return try { block().also { db.setTransactionSuccessful() } } finally { db.endTransaction() }
    }

    private companion object {
        val COLUMNS = arrayOf("id", "url", "title", "updated_at")
    }
}
