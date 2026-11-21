package moe.pure.browser.feature.backup.data

import android.content.Context
import android.util.Base64
import moe.pure.browser.feature.adblock.CustomBlockRules
import moe.pure.browser.feature.adblock.FilterSubscriptions
import moe.pure.browser.feature.scripts.data.ScriptRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.BufferedWriter
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.feature.backup.model.NativeBackupFile

/** The five ordinary Native modules also include an l9-compatible text fallback for older releases. */
internal object LegacyBackupWriter {
    fun exportFile(context: Context, codec: BackupDataCodec, file: File): List<NativeBackupFile> {
        val files = ArrayList<NativeBackupFile>()
        try {
            BufferedWriter(OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)).use { output ->
                output.write(export(context, codec))
                output.flush()
                files.add(NativeBackupFile(file))
            }
        } catch (error: Exception) { NativeLog.warn(null, error) }
        return files
    }

    fun export(context: Context, codec: BackupDataCodec): String {
        fun encode(value: String) = Base64.encodeToString(value.trim().toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        val settingsData = JSONArray(); val sites = JSONObject()
        codec.exportSettings()?.let { text ->
            for (line in text.lineSequence().filter(String::isNotBlank)) {
                val item = JSONObject(line)
                item.optJSONObject("settingsData")?.let(settingsData::put)
                item.optJSONObject("siteConf")?.let { group -> group.keys().forEach { key -> sites.put(key, group.get(key)) } }
            }
        }
        val bookmarks = codec.exportBookmarkRows().joinToString("\n") { row ->
            JSONObject().put("title", row.title.orEmpty()).put("url", row.url.orEmpty()).put("folder", row.folder).put("order", row.order)
                .put("created_at", row.createdAtSeconds).put("updated_at", row.updatedAtSeconds).toString()
        }
        val scripts = ScriptRepository(context).use { repository -> repository.all().asReversed().joinToString("\n") { script ->
            JSONObject().put("scriptId", script.key).put("enabled", script.enabled).put("url", script.downloadUrl)
                .put("codeV2", script.content).toString()
        } }
        val rules = JSONObject()
        CustomBlockRules(context).text().takeIf(String::isNotEmpty)?.let { rules.put("custom", it) }
        FilterSubscriptions(context).use { repository ->
            val rows = repository.all()
            if (rows.isNotEmpty()) rules.put("subscribed", rows.joinToString("\n", postfix = "\n") { JSONObject().put("url", it.url).toString() })
        }
        return JSONObject().put("adrules", encode(rules.toString())).put("bookmark", encode(bookmarks))
            .put("settings", encode(codec.exportSettingValues().toString())).put("other", encode(scripts))
            .put("favorite", encode(codec.exportFavorites().orEmpty())).put("kvdata", encode(settingsData.toString()))
            .put("conf", encode(sites.toString())).toString()
    }
}
