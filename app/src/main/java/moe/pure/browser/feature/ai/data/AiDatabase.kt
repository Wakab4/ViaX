package moe.pure.browser.feature.ai.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import moe.pure.browser.core.logging.NativeLog

/** p9.w opens writableDatabase for reads too; only its explicit batch deletion is transactional. */
internal class AiDatabase(private val store: AiStore) {
    fun <T> write(action: (SQLiteDatabase) -> T): T = action(store.writableDatabase)
    fun <T> list(table: String, selection: String? = null, args: Array<String>? = null,
        order: String? = null, warn: Boolean = false, row: (Cursor) -> T): List<T> {
        val result = ArrayList<T>()
        try { store.writableDatabase.query(table, null, selection, args, null, null, order).use { cursor ->
            while (cursor.moveToNext()) result.add(row(cursor))
        } } catch (error: Exception) { if (warn) NativeLog.warn(null, error) else error.printStackTrace() }
        return result
    }
    fun <T> find(table: String, id: String?, row: (Cursor) -> T): T? {
        var result: T? = null
        try { store.writableDatabase.query(table, null, "id = '$id'", null, null, null, null).use { cursor ->
            if (cursor.moveToNext()) result = row(cursor)
        } } catch (error: Exception) { error.printStackTrace() }
        return result
    }
    fun transaction(action: (SQLiteDatabase) -> Boolean): Boolean {
        val db = store.writableDatabase
        try {
            db.beginTransaction()
            val changed = action(db)
            if (changed) db.setTransactionSuccessful()
            return changed
        } catch (error: Exception) { NativeLog.warn(null, error); return false }
        finally { db.endTransaction() }
    }
}

internal fun Cursor.text(name: String): String = getString(getColumnIndexOrThrow(name)).orEmpty()
internal fun Cursor.nullableText(name: String): String? = getString(getColumnIndexOrThrow(name))
internal fun Cursor.number(name: String): Int = getInt(getColumnIndexOrThrow(name))
internal fun Cursor.time(name: String): Long = getLong(getColumnIndexOrThrow(name))
internal fun String?.aiList(): List<String> {
    val value = this ?: return emptyList()
    if (value.isEmpty()) return emptyList()
    return buildList {
        var start = 0
        while (start < value.length) {
            val separator = value.indexOf(',', start)
            val end = if (separator < 0) value.length else separator
            add(value.substring(start, end).trim { it <= ' ' }); start = end + 1
        }
    }
}
