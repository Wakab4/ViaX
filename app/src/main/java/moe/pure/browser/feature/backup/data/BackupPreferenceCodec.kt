package moe.pure.browser.feature.backup.data

import android.content.Context
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.feature.settings.data.NativeBackupPreferenceKeys
import moe.pure.browser.feature.settings.data.updates.NativePreferenceUpdates
import org.json.JSONObject

/** w9.i/D2 exports only stored Native keys; account and secret fields are outside this list. */
internal class BackupPreferenceCodec(private val context: Context) {
    private val values = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
    fun export(compact: Boolean = false): JSONObject {
        return JSONObject().apply {
            for (key in NativeBackupPreferenceKeys.booleans) if (values.contains(key)) put(key, values.getBoolean(key, false))
            for (key in NativeBackupPreferenceKeys.strings) if (values.contains(key)) {
                val text = values.getString(key, "")
                if (!compact || text!!.length <= 4096) put(key, text)
            }
            for (key in NativeBackupPreferenceKeys.integers) if (values.contains(key)) put(key, values.getInt(key, 0))
            for (key in NativeBackupPreferenceKeys.longs) if (values.contains(key)) put(key, values.getLong(key, 0))
        }
    }
    fun restore(json: JSONObject): Int {
        val count = NativeBackupPreferenceKeys.all.count { !json.isNull(it) }
        BrowserPreferences(values).applyNativeSettings(json)
        NativePreferenceUpdates.apply(context)
        return count
    }
}

