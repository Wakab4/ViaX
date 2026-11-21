package moe.pure.browser.feature.backup.data.filters

import android.content.Context
import moe.pure.browser.core.files.NativeDirectories
import moe.pure.browser.core.files.NativeFileCopy
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.adblock.AdblockEngine
import moe.pure.browser.feature.adblock.FilterSubscription
import moe.pure.browser.feature.adblock.data.NativeFilterSubscriptionStore
import moe.pure.browser.feature.adblock.rules.CustomRuleFiles
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.UUID

/** m9.l.m mutates the original list once, then restores the custom file and schedules reload. */
internal class SettingsFilterRestore(private val context: Context) {
    fun restore(records: List<JSONObject>, files: Map<String, File>): Int {
        val store = NativeFilterSubscriptionStore.get(context)
        val subscriptions = store.all()
        val byUrl = HashMap<String, FilterSubscription>()
        subscriptions.forEach { byUrl[it.url] = it }
        var changed = false
        var count = 0
        for (record in records) {
            val url = record.optString("url")
            if (url.isEmpty()) continue
            val current = byUrl[url]
            val incoming = FilterSubscription(url, record.optString("title").takeIf(String::isNotEmpty),
                enabled = record.optBoolean("enabled", false), count = record.optInt("size", 0))
            val source = files[record.optString("path", UUID.randomUUID().toString())]
            if (current == null) {
                val fileName = UUID.randomUUID().toString() + ".txt"
                incoming.filePath = NativeDirectories.appFiles(context, "filters")?.let { File(it, fileName).toString() }
                if (source != null && source.isFile) NativeFileCopy.copy(source, File(incoming.filePath!!))
                else incoming.count = 0
                byUrl[url] = incoming
                subscriptions.add(incoming)
            } else {
                current.enabled = incoming.enabled
                val updated = if (source != null && source.isFile) record.optLong("lastUpdated", 1) else 0
                if (updated > current.updated) {
                    current.count = incoming.count
                    NativeFileCopy.copy(source, File(current.filePath!!))
                }
            }
            changed = true
            count++
        }
        NativeLog.debug(null, "filter data changed: $changed")
        if (changed) store.publish(subscriptions)
        val target = CustomRuleFiles.file(context)
        val custom = files["filters-custom.txt"]
        if (custom != null && custom.exists()) {
            if (target.exists()) {
                NativeLog.debug(null, "restore filters, combine custom filters")
                val lines = HashSet<String>()
                readLines(custom, lines)
                readLines(target, lines)
                try {
                    BufferedWriter(FileWriter(target)).use { writer ->
                        for (line in lines) { writer.write(line); writer.newLine() }
                    }
                } catch (error: IOException) { NativeLog.warn(null, error) }
            } else {
                NativeLog.debug(null, "restore filters, copy custom filters")
                NativeFileCopy.copy(custom, target)
            }
            changed = true
            count++
        }
        if (changed) {
            NativeLog.debug(null, "restore filters, reload filters")
            BrowserExecutors.io.execute { AdblockEngine.changed(context) }
        }
        return count
    }

    private fun readLines(file: File, lines: MutableSet<String>) {
        try {
            BufferedReader(FileReader(file)).use { reader ->
                while (true) lines.add(reader.readLine() ?: break)
            }
        } catch (error: IOException) { NativeLog.warn(null, error) }
    }
}
