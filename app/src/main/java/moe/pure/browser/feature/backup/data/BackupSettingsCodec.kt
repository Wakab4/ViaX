package moe.pure.browser.feature.backup.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import moe.pure.browser.feature.backup.data.settings.SettingsDataRestore
import moe.pure.browser.feature.backup.data.settings.SettingsStreamRestore
import moe.pure.browser.feature.backup.data.settings.SettingsImageRestore
import moe.pure.browser.feature.backup.data.filters.SettingsFilterRestore
import moe.pure.browser.feature.backup.data.scripts.SettingsScriptRestore
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.feature.backup.data.export.SettingsExportFiles
import moe.pure.browser.feature.backup.data.export.SettingsFileExport
import moe.pure.browser.feature.backup.model.NativeBackupFile
import moe.pure.browser.feature.backup.model.SettingsImportKind
import java.io.BufferedReader
import java.io.File

/** Native settings JSON lines and their script, filter and image attachments. */
internal class BackupSettingsCodec(private val app: Context, private val db: SQLiteDatabase) {
    fun export(directory: File, inline: Boolean): List<NativeBackupFile> =
        SettingsFileExport(app, db).write(SettingsExportFiles(directory, inline))

    fun restore(text: String, attachments: Map<String, File> = emptyMap(), kind: SettingsImportKind = SettingsImportKind.CURRENT_LINES): Int =
        restore({ text.reader().buffered() }, attachments, kind)

    fun restore(file: File, attachments: Map<String, File>, kind: SettingsImportKind): Int =
        if (file.isFile) restore({ file.bufferedReader(Charsets.UTF_8) }, attachments, kind) else 0

    private fun restore(openReader: () -> BufferedReader, attachments: Map<String, File>, kind: SettingsImportKind): Int {
        var count = 0
        val records = SettingsStreamRestore.read(openReader) { item ->
            count += BackupPreferenceCodec(app).restore(item)
            SettingsImageRestore(app).restore(item, attachments)
        }
        if (records.data.isNotEmpty()) {
            NativeLog.debug(null, "restore settings data")
            count += SettingsDataRestore(app, db).restore(records.data)
        }
        if (records.sites.isNotEmpty()) {
            NativeLog.debug(null, "restore site confs")
            count += BackupSiteCodec(db).restoreBatch(records.sites, kind)
        }
        if (records.scripts.isNotEmpty()) {
            NativeLog.debug(null, "restore scripts")
            count += SettingsScriptRestore(app).restore(records.scripts, attachments, kind)
        }
        if (records.filters.isNotEmpty()) {
            NativeLog.debug(null, "restore filters")
            count += SettingsFilterRestore(app).restore(records.filters, attachments)
        }
        return count
    }
}
