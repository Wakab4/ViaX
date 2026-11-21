package moe.pure.browser.feature.backup.data.export

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.data.settings.NativeSettingsRows
import moe.pure.browser.feature.backup.data.BackupSiteCodec
import moe.pure.browser.feature.backup.data.filters.SettingsFilterExport
import moe.pure.browser.feature.backup.data.scripts.SettingsScriptExport
import moe.pure.browser.feature.backup.data.settings.SettingsImageExport
import moe.pure.browser.feature.backup.model.NativeBackupFile
import moe.pure.browser.feature.search.data.SearchPreferences
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

/** m9.l.a writes the actual settings.txt before returning ordered attachment descriptors. */
internal class SettingsFileExport(private val context: Context, private val db: SQLiteDatabase) {
    fun write(target: SettingsExportFiles): List<NativeBackupFile> {
        val file = target.file("settings.txt") ?: return emptyList()
        val files = ArrayList<NativeBackupFile>()
        try {
            BufferedWriter(OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8)).use { output ->
                files.add(NativeBackupFile(file))
                val writer = SettingsLineWriter(output)
                writer.record("meta") { JSONObject().put("dataVersion", 1).put("appVersion", 20260823) }
                SettingsImageExport(context).write(writer, files)
                val rows = NativeSettingsRows(db).all()
                val values = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
                val userAgent = values.getInt("uachoice", 0)
                val search = SearchPreferences(context).engineId
                for (row in rows) writer.record("settingsData") {
                    row.toJson().apply {
                        if (row.type == 1 && row.id == userAgent || row.type == 2 && row.id == search) put("selected", true)
                    }
                }
                for (record in BackupSiteCodec(db).export()) writer.record("siteConf") { record }
                SettingsScriptExport(context).write(writer, files, target)
                SettingsFilterExport(context).write(writer, files, target.inline)
                output.flush()
            }
        } catch (error: IOException) { NativeLog.warn(null, error) }
        return files
    }
}
