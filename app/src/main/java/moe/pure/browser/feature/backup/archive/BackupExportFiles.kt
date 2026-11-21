package moe.pure.browser.feature.backup.archive

import android.content.Context
import moe.pure.browser.feature.backup.data.BackupDataCodec
import moe.pure.browser.feature.backup.data.LegacyBackupWriter
import moe.pure.browser.feature.backup.model.BackupData
import moe.pure.browser.feature.backup.model.BackupFileResult
import moe.pure.browser.feature.backup.model.BackupItemResult
import moe.pure.browser.feature.backup.model.BackupResult
import moe.pure.browser.feature.backup.model.ExportedBackupFiles
import moe.pure.browser.feature.backup.model.NativeBackupFile
import java.io.File

/** m9.n collects each selected module before any archive entry is written. */
internal class BackupExportFiles(private val context: Context) {
    fun collect(directory: File, selection: Set<BackupData>, password: CharArray?): ExportedBackupFiles {
        val files = ArrayList<NativeBackupFile>()
        val items = ArrayList<BackupItemResult>()
        BackupDataCodec(context).use { codec ->
            for (data in BackupData.entries) {
                if (data !in selection) continue
                val target = File(directory, data.fileName)
                val result = when (data) {
                    BackupData.BOOKMARKS -> codec.exportBookmarkFile(target)
                    BackupData.FAVORITES -> codec.exportFavoriteFiles(directory)
                    BackupData.HISTORY -> codec.exportHistoryFile(target)
                    BackupData.CLOSED_TABS -> codec.exportClosedTabsFile(target)
                    BackupData.SETTINGS -> {
                        val settings = codec.exportSettingsFiles(directory)
                        val count = settings.firstOrNull()?.file?.bufferedReader(Charsets.UTF_8)
                            ?.useLines { lines -> lines.count(String::isNotBlank) } ?: 0
                        BackupFileResult(settings, count)
                    }
                    BackupData.PASSWORDS -> PasswordFileExport(context).write(directory, password)
                }
                files.addAll(result.files)
                items.add(BackupItemResult(data, result.count))
            }
            if (selection == BackupData.DEFAULT) files.addAll(LegacyBackupWriter.exportFile(context, codec,
                File(directory, "data-for-older-versions.txt")))
        }
        return ExportedBackupFiles(files, BackupResult(items))
    }
}
