package moe.via.browser.browser.session.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/** Native na.c deletes then inserts, including existing UUIDs, so rowid retains the runtime order. */
internal class TabRows(private val db: SQLiteDatabase) {
    fun get(id: String): TabEntry? = query("_id=?", arrayOf(id)).firstOrNull()

    fun query(where: String? = null, args: Array<String>? = null, order: String = "rowid",
        limit: String? = null): List<TabEntry> = db.query("tabs", COLUMNS, where, args, null, null, order, limit).use { cursor ->
        buildList { while (cursor.moveToNext()) add(cursor.entry()) }
    }

    fun delete(id: String): Int = db.delete("tabs", "_id=?", arrayOf(id))

    fun insert(entry: TabEntry) {
        delete(entry.id)
        db.insertOrThrow("tabs", null, values(entry))
    }

    /** Closed exact-URL replacement does not touch an open row with the same URL. */
    fun put(entry: TabEntry): List<TabEntry> {
        val removed = if ((entry.flags and TabFlags.OPEN) == 0)
            query("_id<>? AND url=? AND (flags & 2)=0", arrayOf(entry.id, entry.url)) else emptyList()
        removed.forEach { delete(it.id) }
        insert(entry)
        return removed
    }

    fun <T> transaction(block: () -> T): T {
        // Backup archive imports already own a database transaction.
        if (db.inTransaction()) return block()
        db.beginTransaction()
        return try { block().also { db.setTransactionSuccessful() } }
        finally { db.endTransaction() }
    }

    private fun Cursor.entry() = TabEntry(
        id = getString(0), url = getString(1).orEmpty(), title = getString(2).orEmpty(),
        filePath = getString(3), flags = getInt(4), lastVisitedAt = getLong(5),
        home = getInt(6) != 0, desktop = getInt(7) != 0,
        scrollX = getInt(8), scrollY = getInt(9)
    )

    companion object {
        private val COLUMNS = arrayOf("_id", "url", "title", "file_path", "flags", "last_visited_at",
            "home", "desktop", "scroll_x", "scroll_y")

        fun values(entry: TabEntry) = ContentValues().apply {
            put("_id", entry.id); put("url", entry.url); put("title", entry.title)
            put("file_path", entry.filePath); put("flags", entry.flags); put("last_visited_at", entry.lastVisitedAt)
            put("home", if (entry.home) 1 else 0); put("desktop", if (entry.desktop) 1 else 0)
            put("scroll_x", entry.scrollX); put("scroll_y", entry.scrollY)
        }
    }
}
