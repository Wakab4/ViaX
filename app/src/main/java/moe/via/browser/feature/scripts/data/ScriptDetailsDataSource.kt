package moe.via.browser.feature.scripts.data

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.scripts.model.ScriptDetailsMetadata
import moe.via.browser.feature.scripts.model.ScriptDetailsScope
import org.json.JSONArray
import org.json.JSONObject

/** p5.b.k/q/m/l0/n: sparse readable queries, original nulls and per-query error paths. */
internal class ScriptDetailsDataSource(private val database: () -> SQLiteDatabase) {
    fun metadata(id: Int) = metadata("_id = $id", null)
    fun metadata(key: String) = metadata("script_id = ?", arrayOf(key))
    private fun metadata(where: String, args: Array<String>?): ScriptDetailsMetadata? {
        var value: ScriptDetailsMetadata? = null
        try {
            database().query("scripts", META_COLUMNS, where, args, null, null, null).use { cursor ->
                if (cursor.moveToNext()) value = readMetadata(cursor)
            }
        } catch (error: Exception) { error.printStackTrace() }
        return value
    }
    private fun readMetadata(cursor: Cursor): ScriptDetailsMetadata? = try {
        fun text(name: String): String? = cursor.getString(cursor.getColumnIndexOrThrow(name))
        fun number(name: String) = cursor.getLong(cursor.getColumnIndexOrThrow(name))
        // k0 selects support_url but leaves the metadata field unset.
        ScriptDetailsMetadata(number("_id").toInt(), text("name"), text("version"), cursor.getInt(cursor.getColumnIndexOrThrow("enabled")) == 1,
            text("icon_url"), text("homepage_url"), text("download_url"), number("last_updated_at"), number("created_at"))
    } catch (error: Exception) { error.printStackTrace(); null }

    fun defaults(id: Int): ScriptDetailsScope? = try {
        database().query("scripts", arrayOf("run_at", "matches", "exclude_matches", "includes", "excludes"),
            "_id = $id", null, null, null, null).use { cursor ->
            if (!cursor.moveToNext()) null else ScriptDetailsScope(cursor.getInt(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4))
        }
    } catch (error: Exception) { error.printStackTrace(); null }

    fun overrides(id: Int): ScriptDetailsScope? {
        var value: String? = null
        try {
            database().query("scripts", arrayOf("user_overrides"), "_id = $id", null, null, null, null).use { cursor ->
                if (cursor.moveToNext()) value = cursor.getString(0)
            }
        } catch (error: Exception) { error.printStackTrace() }
        if (value.isNullOrEmpty()) return null
        return try {
            val json = JSONObject(value!!)
            ScriptDetailsScope(json.optInt("runAt", 0), json.optString("matches"), json.optString("excludeMatches"), json.optString("includes"), json.optString("excludes"))
        } catch (error: Exception) { NativeLog.warn("e", error); null }
    }

    companion object {
        private val META_COLUMNS = arrayOf("_id", "name", "version", "enabled", "icon_url", "homepage_url", "support_url", "download_url", "last_updated_at", "created_at")
        /** s5.e.a/b keeps optString conversions and an accumulated prefix on read errors. */
        fun scopes(first: String?, second: String?): List<String> {
            fun read(value: String?): List<String> {
                val result = ArrayList<String>()
                if (!value.isNullOrEmpty()) try {
                    val array = JSONArray(value)
                    for (index in 0 until array.length()) result.add(array.optString(index))
                } catch (error: Exception) { NativeLog.warn("e", error) }
                return result
            }
            if (first.isNullOrEmpty()) return read(second)
            if (second.isNullOrEmpty()) return read(first)
            return read(first) + read(second)
        }
    }
}
