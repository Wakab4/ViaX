package moe.pure.browser.feature.backup.data.export

import moe.pure.browser.core.logging.NativeLog
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.Writer

/** m9.l.q and its per-record IOException/JSONException boundaries. */
internal class SettingsLineWriter(private val writer: Writer) {
    fun record(key: String, create: () -> JSONObject?) {
        try {
            val value = create()
            if (key.isEmpty() || value == null || value.length() == 0) return
            writer.write(JSONObject().put(key, value).toString())
            writer.write("\n")
        } catch (error: IOException) { NativeLog.warn(null, error) }
        catch (error: JSONException) { NativeLog.warn(null, error) }
    }
}
