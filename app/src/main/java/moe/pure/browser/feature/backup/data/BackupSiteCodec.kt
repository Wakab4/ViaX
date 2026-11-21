package moe.pure.browser.feature.backup.data

import android.database.sqlite.SQLiteDatabase
import moe.pure.browser.feature.backup.data.site.SiteBackupRecords
import moe.pure.browser.feature.backup.model.SettingsImportKind
import moe.pure.browser.feature.site.data.NativeSiteConfigCodec
import moe.pure.browser.feature.site.data.NativeSiteStores
import org.json.JSONObject

/** Native siteConf values use the active store's raw masks and literal keys. */
internal class BackupSiteCodec(private val db: SQLiteDatabase) {
    fun export(): List<JSONObject> {
        val store = NativeSiteStores.get(db)
        store.reload(db)
        val records = store.snapshot(db)
        return records.mapNotNull { (key, config) ->
            if (key.isEmpty()) return@mapNotNull null
            if (config.isEmpty()) return@mapNotNull null
            val json = NativeSiteConfigCodec.encode(config)
            if (config.userAgentChoice > 0) db.rawQuery("SELECT content FROM settings WHERE id=?",
                arrayOf(config.userAgentChoice.toString())).use { cursor ->
                if (cursor.moveToFirst()) json.put("uafallback", cursor.getString(0))
            }
            JSONObject().put(key, json)
        }
    }

    fun restoreBatch(records: List<JSONObject>, kind: SettingsImportKind): Int {
        if (records.isEmpty()) return 0
        val currentAgents = if (kind == SettingsImportKind.LEGACY_CONF) emptyMap() else currentUserAgents()
        val restored = SiteBackupRecords(currentAgents).collect(records, kind)
        NativeSiteStores.get(db).batch(db, restored)
        return restored.size
    }

    private fun currentUserAgents(): Map<String, Int> = db.query("settings", arrayOf("id", "content"),
        "type = ?", arrayOf("1"), null, null, "id").use { cursor ->
        buildMap {
            while (cursor.moveToNext()) cursor.getString(1)?.let { put(it, cursor.getInt(0)) }
        }
    }
}
