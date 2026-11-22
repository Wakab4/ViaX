package moe.via.browser.feature.backup.data.settings

import moe.via.browser.core.logging.NativeLog
import org.json.JSONException
import org.json.JSONObject

/** m9.l.j reads the first outer key and rejects empty record objects. */
internal object SettingsBackupLine {
    fun read(line: String): Pair<String, JSONObject>? = try {
        val outer = JSONObject(line)
        val keys = outer.keys()
        if (!keys.hasNext()) null else {
            val key = keys.next()
            val value = outer.optJSONObject(key)
            if (key.isEmpty() || value == null || value.length() == 0) null else key to value
        }
    } catch (error: JSONException) { NativeLog.warn(null, error); null }
}
