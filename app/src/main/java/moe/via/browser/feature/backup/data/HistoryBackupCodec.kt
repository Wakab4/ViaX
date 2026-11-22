package moe.via.browser.feature.backup.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import moe.via.browser.feature.backup.model.ImportMode
import moe.via.browser.feature.library.history.data.HistoryRecord
import moe.via.browser.feature.library.history.data.HistoryRows
import moe.via.browser.feature.home.data.HomePreferences
import org.json.JSONObject
import java.io.File
import moe.via.browser.feature.backup.data.export.NativeBackupJsonFile
import moe.via.browser.feature.backup.model.BackupFileResult
import moe.via.browser.feature.backup.model.NativeBackupFile

/** m9.g's ordinary history.txt module; archived Native fields stay in seconds. */
internal class HistoryBackupCodec(private val app: Context, db: SQLiteDatabase) {
    private val rows = HistoryRows(db)

    fun export(): String? {
        val records = rows.read().rows
        if (records.isEmpty()) return null
        return buildString {
            for (record in records) {
                val item = JSONObject().put("title", record.title.orEmpty()).put("url", record.url.orEmpty())
                    .put("updated_at", record.updatedAtSeconds)
                append(item).append('\n')
            }
        }
    }

    fun exportFile(file: File): BackupFileResult {
        val records = rows.read().rows
        if (records.isEmpty()) return BackupFileResult(emptyList(), 0)
        val written = NativeBackupJsonFile.write(file, records) { record ->
            JSONObject().put("title", record.title).put("url", record.url).put("updated_at", record.updatedAtSeconds)
        }
        return if (written) BackupFileResult(listOf(NativeBackupFile(file)), records.size) else BackupFileResult(emptyList(), 0)
    }

    fun restore(text: String, mode: ImportMode): Int {
        val records = mutableListOf<HistoryRecord>()
        for (line in text.lineSequence()) {
            if (line.isEmpty()) continue
            try {
                val item = JSONObject(line)
                val title = item.getString("title")
                val url = item.getString("url")
                if (url.isNotEmpty()) records.add(HistoryRecord(url = url, title = title,
                    updatedAtSeconds = item.optLong("updated_at")))
            } catch (error: org.json.JSONException) { Log.w("History", "History backup record", error) }
        }
        val count = if (records.isEmpty()) 0 else {
            // Native clears separately before importing; the archive caller supplies MERGE (0).
            if (mode == ImportMode.REPLACE) rows.clear()
            rows.importNative(records)
        }
        // m9.g marks w9.n.w/home bit 1, rather than the visit writer's history bit 16.
        HomePreferences(app).markDirty(html = true, style = false)
        return count
    }

}
