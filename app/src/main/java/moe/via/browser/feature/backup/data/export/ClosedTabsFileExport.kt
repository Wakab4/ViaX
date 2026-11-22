package moe.via.browser.feature.backup.data.export

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import moe.via.browser.browser.session.data.TabRepository
import moe.via.browser.feature.backup.model.BackupFileResult
import moe.via.browser.feature.backup.model.NativeBackupFile
import org.json.JSONObject
import java.io.File

/** m9.m writes the current closed-tab snapshot without materializing the whole text first. */
internal class ClosedTabsFileExport(private val context: Context, private val db: SQLiteDatabase) {
    fun write(file: File): BackupFileResult {
        val records = TabRepository(context, db).use { it.exportClosed() }
        if (records.isEmpty()) return BackupFileResult(emptyList(), 0)
        val written = NativeBackupJsonFile.write(file, records) { record ->
            JSONObject().put("title", record.title).put("url", record.url).put("updated_at", record.lastVisitedAt)
        }
        return if (written) BackupFileResult(listOf(NativeBackupFile(file)), records.size) else BackupFileResult(emptyList(), 0)
    }
}
