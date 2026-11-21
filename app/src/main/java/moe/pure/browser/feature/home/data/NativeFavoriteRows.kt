package moe.pure.browser.feature.home.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import moe.pure.browser.feature.home.model.HomeFavorite
import java.util.Collections
import moe.pure.browser.feature.search.resolve.SearchUrlResolver

/** fa.b owns the raw favorite schema and original transaction result, without UI text conversion. */
internal class NativeFavoriteRows(private val db: SQLiteDatabase) {
    companion object {
        fun create(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE favorites(f_id INTEGER PRIMARY KEY,f_url TEXT,f_title TEXT,f_order INTEGER,updated_at INTEGER DEFAULT 0,created_at INTEGER DEFAULT 0)")
        }
    }

    fun all(): MutableList<HomeFavorite> {
        val result = ArrayList<HomeFavorite>()
        try {
            db.query("favorites", arrayOf("f_id", "f_url", "f_title", "f_order"), null, null, null, null, "f_order").use { cursor ->
                while (cursor.moveToNext()) result.add(HomeFavorite(id = cursor.getInt(0), url = cursor.getString(1), title = cursor.getString(2), position = cursor.getInt(3)))
            }
        } catch (error: Exception) { error.printStackTrace() }
        return result
    }

    fun get(id: Int): HomeFavorite? {
        if (id < 0) return null
        var result: HomeFavorite? = null
        try {
            db.query("favorites", arrayOf("f_url", "f_title", "f_order"), "f_id = ?", arrayOf(id.toString()), null, null, null, "1").use { cursor ->
                if (cursor.moveToNext()) {
                    val row = HomeFavorite(id = id)
                    result = row
                    row.rawUrl = cursor.getString(0); row.rawTitle = cursor.getString(1); row.position = cursor.getInt(2)
                }
            }
        } catch (error: Exception) { error.printStackTrace() }
        return result
    }

    fun find(raw: String?): HomeFavorite? {
        if (raw == null) return null
        val url = raw.trim { it <= ' ' }
        if (url.isEmpty()) return null
        val variants = HomeFavoriteUrls.equivalents(url)
        var result: HomeFavorite? = null
        try {
            db.query("favorites", arrayOf("f_id", "f_url", "f_title", "f_order"),
                "f_url in (${variants.joinToString(",") { "?" }})", variants.toTypedArray(), null, null, null, "1").use { cursor ->
                if (cursor.moveToNext()) {
                    val row = HomeFavorite()
                    result = row
                    row.id = cursor.getInt(0); row.rawUrl = cursor.getString(1); row.rawTitle = cursor.getString(2); row.position = cursor.getInt(3)
                }
            }
        } catch (error: Exception) { error.printStackTrace() }
        return result
    }

    fun delete(id: Int) = db.delete("favorites", "f_id = ?", arrayOf(id.toString()))

    fun deleteUrl(raw: String) {
        val url = raw.trim { it <= ' ' }
        if (url.isEmpty()) return
        val variants = HomeFavoriteUrls.equivalents(url)
        db.delete("favorites", "f_url in (${variants.joinToString(",") { "?" }})", variants.toTypedArray())
    }

    fun clear() { db.delete("favorites", null, null) }

    fun search(raw: String, limit: Int): List<HomeFavorite> {
        val result = ArrayList<HomeFavorite>()
        if (raw.isEmpty()) return result
        val escaped = '%' in raw || '_' in raw
        val text = if (escaped) raw.replace("%", "\\%").replace("_", "\\_") else raw
        val escape = if (escaped) " escape '\\'" else ""
        val selection: String
        val args: Array<String>
        if (SearchUrlResolver.isHttp(text)) {
            selection = "f_url like ?$escape"
            args = arrayOf("$text%")
        } else {
            selection = "f_url like ?$escape or f_title like ?$escape or f_title like ?$escape"
            args = arrayOf("%_$text%", "%_$text%", "$text%")
        }
        try {
            db.query("favorites", arrayOf("f_id", "f_url", "f_title"), selection, args, null, null, "f_id desc",
                if (limit <= 0) null else limit.toString()).use { cursor ->
                while (cursor.moveToNext()) result.add(HomeFavorite(id = cursor.getInt(0), url = cursor.getString(1), title = cursor.getString(2)))
            }
        } catch (error: Exception) { error.printStackTrace() }
        return result
    }

    fun restoreBatch(records: List<HomeFavorite>) {
        try {
            db.beginTransaction()
            for (record in records) {
                db.delete("favorites", "f_url = ?", arrayOf(java.lang.String.valueOf(record.url)))
                val values = ContentValues().apply { put("f_url", record.url); put("f_title", record.title); put("f_order", record.position) }
                db.insert("favorites", null, values)
            }
            db.setTransactionSuccessful()
        } catch (error: Exception) { error.printStackTrace() }
        finally { db.endTransaction() }
    }

    fun move(from: Int, to: Int) {
        val records = all()
        try {
            var position = from
            while (position < to) { Collections.swap(records, position, position + 1); position++ }
            while (position > to) { Collections.swap(records, position, position - 1); position-- }
            db.beginTransaction()
            val values = ContentValues()
            records.forEachIndexed { index, record ->
                values.put("f_order", index)
                db.update("favorites", values, "f_id = ?", arrayOf(record.id.toString()))
            }
            db.setTransactionSuccessful()
        } catch (error: Exception) { error.printStackTrace() }
        finally { db.endTransaction() }
    }

    fun save(item: HomeFavorite?): Int {
        if (item == null || item.hasEmptyUrl) return -1
        val inserting = item.id <= 0
        var result = -1
        var order = item.position
        try {
            db.beginTransaction()
            if (inserting) db.delete("favorites", "f_url = ?", arrayOf(item.url))
            if (inserting && order == -1) {
                try {
                    db.rawQuery("select max(f_order) from favorites", null).use { rows ->
                        if (rows.moveToNext()) order = rows.getInt(0) + 1
                    }
                } catch (error: Exception) { error.printStackTrace() }
            }
            val now = System.currentTimeMillis() / 1000
            val values = ContentValues().apply {
                put("f_url", item.url); put("f_title", item.title); put("f_order", order); put("updated_at", now)
            }
            if (inserting) {
                values.put("created_at", now)
                result = db.insert("favorites", null, values).toInt()
            } else if (db.update("favorites", values, "f_id = ?", arrayOf(item.id.toString())) > 0) {
                result = item.id
            }
            db.setTransactionSuccessful()
        } catch (error: Exception) { error.printStackTrace() }
        finally { db.endTransaction() }
        return result
    }
}
