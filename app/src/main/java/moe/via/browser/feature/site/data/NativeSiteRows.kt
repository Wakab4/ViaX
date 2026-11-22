package moe.via.browser.feature.site.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

/** da.a's literal BINARY keys and serialized Native configurations. */
internal class NativeSiteRows(private val db: SQLiteDatabase) {
    fun read(): Map<String, NativeSiteConfig> = db.query("conf", arrayOf("domain", "data"), null, null, null, null, null).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) {
                val key = cursor.getString(0)
                val config = cursor.getString(1)?.let(NativeSiteConfigCodec::decode)
                if (!key.isNullOrEmpty() && config != null) put(key, config)
            }
        }
    }
    fun write(key: String, config: NativeSiteConfig?) {
        delete(key)
        if (config != null && !config.isEmpty()) db.insertOrThrow("conf", null, ContentValues().apply {
            put("domain", key); put("data", NativeSiteConfigCodec.encode(config).toString())
        })
    }
    fun delete(key: String) { db.delete("conf", "domain=? COLLATE BINARY", arrayOf(key)) }
    fun rename(old: String, new: String): SiteRenameResult = transaction {
        if (db.rawQuery("SELECT 1 FROM conf WHERE domain=? COLLATE BINARY", arrayOf(new)).use { it.moveToFirst() })
            return@transaction SiteRenameResult.CONFLICT
        val count = db.update("conf", ContentValues().apply { put("domain", new) }, "domain=? COLLATE BINARY", arrayOf(old))
        if (count == 1) SiteRenameResult.RENAMED else SiteRenameResult.MISSING
    }
    fun <T> transaction(block: () -> T): T {
        if (db.inTransaction()) return block()
        db.beginTransaction()
        return try { block().also { db.setTransactionSuccessful() } } finally { db.endTransaction() }
    }
}
