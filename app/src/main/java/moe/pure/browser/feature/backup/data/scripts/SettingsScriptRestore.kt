package moe.pure.browser.feature.backup.data.scripts

import android.content.Context
import moe.pure.browser.core.files.NativeTextFiles
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.core.text.NativeCompressedText
import moe.pure.browser.feature.backup.model.SettingsImportKind
import moe.pure.browser.feature.scripts.data.ScriptCodec
import moe.pure.browser.feature.scripts.data.ScriptInstaller
import moe.pure.browser.feature.scripts.data.ScriptRepository
import moe.pure.browser.feature.scripts.model.ScriptParser
import moe.pure.browser.feature.scripts.model.UserScript
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.UUID

/** The caller selects the actual local-file or cloud format; payload keys never select a format. */
internal class SettingsScriptRestore(private val context: Context) {
    fun restore(records: List<JSONObject>, files: Map<String, File>, kind: SettingsImportKind): Int {
        val count = when (kind) {
            SettingsImportKind.CURRENT_LINES -> restoreCurrent(records, files)
            SettingsImportKind.LEGACY_CONF -> restoreCloud(records)
        }
        if (count > 0) {
            NativeLog.debug(null, "download all scripts dependencies")
            ScriptInstaller.prepareAllDependencies(context)
        }
        return count
    }

    private fun restoreCurrent(records: List<JSONObject>, files: Map<String, File>): Int {
        var count = 0
        ScriptRepository(context).use { repository ->
            for (record in records) {
                val key = record.optString("scriptId")
                val url = record.optString("url")
                val old = repository.findForBackup(key, url)
                val enabled = record.optBoolean("enabled", false)
                if (old != null && record.optLong("lastUpdated", 1) < old.updated) {
                    if (enabled != old.enabled) repository.enable(old.id, enabled)
                    continue
                }
                var code: String? = record.optString("code")
                if (!code.isNullOrEmpty()) {
                    try { code = NativeCompressedText.decode(code) } catch (_: IOException) { continue }
                } else {
                    val file = files[record.optString("path", UUID.randomUUID().toString())]
                    if (file != null && file.isFile) code = NativeTextFiles.read(file)
                }
                val parsed = ScriptParser.parse(code.orEmpty(), url.takeIf(String::isNotEmpty))
                val script = if (parsed != null) parsed else {
                    if (url.isEmpty() || old != null) continue
                    UserScript(key = key, name = record.optString("name").ifEmpty { url }, content = "",
                        downloadUrl = url, runAt = 0, updated = 0, created = 0)
                }
                val overrides = record.optString("userOverrides").takeIf(String::isNotEmpty)
                val value = script.copy(enabled = enabled,
                    overrides = if (overrides == null) script.overrides else ScriptCodec.overrides(overrides))
                count++
                repository.restoreFromBackup(value, old, overrides)
            }
        }
        return count
    }

    /** l9.h parses all converted cloud records before assigning timestamps eight milliseconds apart. */
    private fun restoreCloud(records: List<JSONObject>): Int {
        val parsed = ArrayList<UserScript>()
        for (record in records) {
            val script = ScriptParser.parse(record.optString("pureCode"), record.optString("url").takeIf(String::isNotEmpty)) ?: continue
            parsed.add(script.copy(key = record.optString("scriptId").takeIf(String::isNotEmpty) ?: script.key,
                enabled = record.optBoolean("enabled", false)))
        }
        if (parsed.isEmpty()) return 0
        var timestamp = System.currentTimeMillis()
        ScriptRepository(context).use { repository ->
            for (script in parsed) {
                val value = script.copy(created = timestamp, updated = timestamp)
                timestamp += 8L
                repository.restoreFromBackup(value, repository.findForBackup(value.key, value.downloadUrl.orEmpty()), null)
            }
        }
        return parsed.size
    }
}
