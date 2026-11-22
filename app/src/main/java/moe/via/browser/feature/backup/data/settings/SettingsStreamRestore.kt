package moe.via.browser.feature.backup.data.settings

import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException

internal data class SettingsRestoreRecords(
    val data: List<JSONObject>,
    val sites: List<JSONObject>,
    val scripts: List<JSONObject>,
    val filters: List<JSONObject>
)

/** m9.l.b applies settings while reading, then dispatches the accumulated domain records. */
internal object SettingsStreamRestore {
    fun read(openReader: () -> BufferedReader, restoreSettings: (JSONObject) -> Unit): SettingsRestoreRecords {
        val data = ArrayList<JSONObject>()
        val sites = ArrayList<JSONObject>()
        val scripts = ArrayList<JSONObject>()
        val filters = ArrayList<JSONObject>()
        var acceptedSettings = 0
        try {
            openReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    val (key, value) = SettingsBackupLine.read(line) ?: continue
                    when (key) {
                        "settings" -> {
                            restoreSettings(value)
                            acceptedSettings++
                        }
                        "settingsData" -> data.add(value)
                        "siteConf" -> sites.add(value)
                        "script" -> scripts.add(value)
                        "filters" -> filters.add(value)
                    }
                }
            }
        } catch (error: IOException) {
            NativeLog.warn(null, error)
        }
        if (acceptedSettings > 0) {
            GeneratedPageDirtyState.markAll()
            GeneratedPageDirtyState.markSecondary()
        }
        return SettingsRestoreRecords(data, sites, scripts, filters)
    }
}
