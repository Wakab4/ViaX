package moe.via.browser.feature.backup.data.scripts

import android.content.Context
import moe.via.browser.core.files.NativeTextFiles
import moe.via.browser.core.text.NativeCompressedText
import moe.via.browser.feature.backup.data.export.SettingsAttachmentNames
import moe.via.browser.feature.backup.data.export.SettingsExportFiles
import moe.via.browser.feature.backup.data.export.SettingsLineWriter
import moe.via.browser.feature.backup.model.NativeBackupFile
import moe.via.browser.feature.scripts.data.ScriptRepository
import org.json.JSONObject

/** m9.l.f exports reverse metadata order and accepts only keys in the original execution cache. */
internal class SettingsScriptExport(private val context: Context) {
    fun write(writer: SettingsLineWriter, files: MutableList<NativeBackupFile>, target: SettingsExportFiles) {
        ScriptRepository(context).use { repository ->
            for (script in repository.backupTargets().asReversed()) {
                val key = repository.backupKey(script.id)
                if (key.isNullOrEmpty()) continue
                writer.record("script") {
                    val record = JSONObject().put("scriptId", key)
                    if (!script.url.isNullOrEmpty()) record.put("url", script.url)
                    if (!script.name.isNullOrEmpty()) record.put("name", script.name)
                    if (script.enabled) record.put("enabled", true)
                    repository.backupOverrides(script.id)?.let { record.put("userOverrides", it) }
                    val file = target.file("script-${SettingsAttachmentNames.prefix(script.name)}${script.id}.txt")
                    if (file == null) {
                        if (target.inline && script.url.isNullOrEmpty()) repository.backupBody(script.id)?.takeIf(String::isNotEmpty)?.let { code ->
                            record.put("code", NativeCompressedText.encode(code))
                            record.put("lastUpdated", script.updated)
                        }
                    } else {
                        repository.backupBody(script.id)?.takeIf(String::isNotEmpty)?.let { code ->
                            NativeTextFiles.write(file, code)
                            record.put("path", file.name)
                            record.put("lastUpdated", script.updated)
                            files.add(NativeBackupFile(file))
                        }
                    }
                    record
                }
            }
        }
    }
}
