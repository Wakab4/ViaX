package moe.via.browser.feature.backup.data.settings

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import moe.via.browser.storage.database.settings.NativeSettingsRow
import moe.via.browser.storage.database.settings.NativeSettingsRows
import org.json.JSONObject

/** m9.l.o parses before mutation, restores the batch, then resolves selection by content/type. */
internal class SettingsDataRestore(context: Context, db: SQLiteDatabase) {
    private val values = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
    private val store = NativeSettingsRows(db)

    fun restore(records: List<JSONObject>): Int {
        if (records.isEmpty()) return 0
        val rows = ArrayList<NativeSettingsRow>()
        val selected = ArrayList<NativeSettingsRow>()
        val searchUrls = HashMap<Int, String?>()
        for (record in records) {
            val row = NativeSettingsRow.fromJson(record) ?: continue
            rows.add(row)
            if (row.type == 2 && row.id > 0) searchUrls[row.id] = row.content
            if (record.optBoolean("selected", false)) selected.add(row)
        }
        if (rows.isNotEmpty()) store.restoreBatch(rows)
        val mapped = HashMap<Int, Int>()
        for ((id, content) in searchUrls) store.find(content, 2)?.let { mapped[id] = it.id }
        values.edit().putString("searchshortcuts", SettingsDataRemap.shortcuts(values.getString("searchshortcuts", ""), mapped)).apply()
        values.edit().putString("searchtoolbarorder", SettingsDataRemap.list(values.getString("searchtoolbarorder", ""), mapped)).apply()
        values.edit().putString("searchtoolbardisabled", SettingsDataRemap.list(values.getString("searchtoolbardisabled", null), mapped)).apply()
        for (row in selected) {
            val current = store.find(row.content, row.type) ?: continue
            when (current.type) {
                1 -> values.edit().putInt("uachoice", current.id).apply()
                2 -> values.edit().putInt("search2", current.id).apply()
            }
        }
        return rows.size
    }
}
