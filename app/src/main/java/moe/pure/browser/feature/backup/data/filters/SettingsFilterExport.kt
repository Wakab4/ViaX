package moe.pure.browser.feature.backup.data.filters

import android.content.Context
import moe.pure.browser.feature.adblock.FilterSubscriptions
import moe.pure.browser.feature.adblock.rules.CustomRuleFiles
import moe.pure.browser.feature.backup.data.export.SettingsAttachmentNames
import moe.pure.browser.feature.backup.data.export.SettingsLineWriter
import moe.pure.browser.feature.backup.model.NativeBackupFile
import org.json.JSONObject
import java.io.File

/** m9.l.d omits false/empty fields and aliases files using the URL digest. */
internal class SettingsFilterExport(private val context: Context) {
    fun write(writer: SettingsLineWriter, files: MutableList<NativeBackupFile>, inline: Boolean) {
        FilterSubscriptions(context).use { subscriptions ->
            for (subscription in subscriptions.all()) {
                if (subscription.url.isEmpty()) continue
                writer.record("filters") {
                    val record = JSONObject().put("url", subscription.url)
                    if (!subscription.title.isNullOrEmpty()) record.put("title", subscription.title)
                    if (subscription.enabled) record.put("enabled", true)
                    val file = subscription.filePath?.takeIf(String::isNotEmpty)?.let(::File)
                    if (!inline && file != null && file.isFile && file.length() > 0) {
                        val name = "filters-${SettingsAttachmentNames.prefix(subscription.title)}${SettingsAttachmentNames.urlDigest(subscription.url)}.txt"
                        record.put("path", name)
                        files.add(NativeBackupFile(file, name))
                        record.put("size", subscription.count)
                        record.put("lastUpdated", subscription.updated)
                    }
                    record
                }
            }
        }
        if (!inline) CustomRuleFiles.file(context).takeIf(File::exists)?.let { files.add(NativeBackupFile(it, "filters-custom.txt")) }
    }
}
