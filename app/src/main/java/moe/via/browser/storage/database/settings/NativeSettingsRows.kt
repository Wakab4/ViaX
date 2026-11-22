package moe.via.browser.storage.database.settings

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import moe.via.browser.core.logging.NativeLog

/** ja.a owns the original shared settings table; domain repositories only convert their models. */
internal class NativeSettingsRows(private val db: SQLiteDatabase) {
    companion object {
        fun create(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE settings(id INTEGER PRIMARY KEY,title TEXT,content TEXT,note TEXT," +
                "flag INTEGER DEFAULT 0,type INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,created_at INTEGER DEFAULT 0)")
        }
    }

    fun all(type: Int? = null): List<NativeSettingsRow> {
        val rows = ArrayList<NativeSettingsRow>()
        try {
            db.query("settings", null, if (type == null) null else "type = ?",
                type?.let { arrayOf(it.toString()) }, null, null, null).use { cursor ->
                while (cursor.moveToNext()) read(cursor)?.let(rows::add)
            }
        } catch (error: Exception) { error.printStackTrace() }
        return rows
    }

    fun get(id: Int): NativeSettingsRow? {
        var row: NativeSettingsRow? = null
        try {
            db.query("settings", null, "id = ?", arrayOf(id.toString()), null, null, null, "1").use {
                if (it.moveToNext()) row = read(it)
            }
        } catch (error: Exception) { error.printStackTrace() }
        return row
    }

    fun find(content: String?, type: Int): NativeSettingsRow? {
        if (content.isNullOrEmpty()) return null
        try {
            db.query("settings", null, "type = ? and content = ?", arrayOf(type.toString(), content), null, null, null, "1").use {
                if (it.moveToFirst()) return read(it)
            }
        } catch (error: Exception) { NativeLog.warn(null, error) }
        return null
    }

    fun insert(row: NativeSettingsRow?): Int = if (row == null) 0 else db.insert("settings", null, values(row)).toInt()
    fun update(id: Int, row: NativeSettingsRow): Boolean = db.update("settings", values(row), "id = ?", arrayOf(id.toString())) > 0
    fun delete(id: Int): Boolean = db.delete("settings", "id = ?", arrayOf(id.toString())) > 0

    /** ja.a.g commits the normal loop even when duplicates/nulls make its return value false. */
    fun restoreBatch(rows: List<NativeSettingsRow?>): Boolean {
        if (rows.isEmpty()) return false
        var inserted = 0
        try {
            try {
                db.beginTransaction()
                for (row in rows) {
                    if (row == null) continue
                    row.id = 0
                    if (row.type == 1 || row.type == 2) {
                        try {
                            val duplicate = db.query("settings", arrayOf("content"), "type = ? and content = ?",
                                arrayOf(row.type.toString(), row.content), null, null, null, "1").use { it.moveToFirst() }
                            if (duplicate) continue
                        } catch (error: Exception) { NativeLog.warn(null, error) }
                        db.delete("settings", "content = ?", arrayOf(row.content))
                    }
                    if (db.insert("settings", null, values(row)) > 0) inserted++
                }
                db.setTransactionSuccessful()
            } catch (error: Exception) { error.printStackTrace() }
        } finally { db.endTransaction() }
        return inserted == rows.size
    }

    private fun values(row: NativeSettingsRow) = ContentValues().apply {
        if (row.id > 0) put("id", row.id)
        put("title", row.title?.takeIf(String::isNotEmpty)); put("content", row.content?.takeIf(String::isNotEmpty))
        put("note", row.note?.takeIf(String::isNotEmpty))
        put("flag", row.flag); put("type", row.type); put("updated_at", row.updatedAt); put("created_at", row.createdAt)
    }

    private fun read(cursor: Cursor): NativeSettingsRow? = try {
        fun text(column: String) = cursor.getString(cursor.getColumnIndexOrThrow(column))?.takeIf(String::isNotEmpty)
        NativeSettingsRow(cursor.getInt(cursor.getColumnIndexOrThrow("id")), text("title"), text("content"), text("note"),
            cursor.getInt(cursor.getColumnIndexOrThrow("flag")), cursor.getInt(cursor.getColumnIndexOrThrow("type")),
            cursor.getLong(cursor.getColumnIndexOrThrow("updated_at")), cursor.getLong(cursor.getColumnIndexOrThrow("created_at")))
    } catch (error: Exception) { NativeLog.warn(null, error); null }
}
