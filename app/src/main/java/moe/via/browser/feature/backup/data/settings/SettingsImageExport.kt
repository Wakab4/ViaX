package moe.via.browser.feature.backup.data.settings

import android.content.Context
import moe.via.browser.feature.backup.data.BackupPreferenceCodec
import moe.via.browser.feature.backup.data.export.SettingsLineWriter
import moe.via.browser.feature.backup.model.NativeBackupFile
import moe.via.browser.feature.home.data.HomePreferences
import moe.via.browser.feature.home.model.LogoMode
import java.io.File

/** m9.l.c adds existing image attachments even for an inline sync target. */
internal class SettingsImageExport(private val context: Context) {
    fun write(writer: SettingsLineWriter, files: MutableList<NativeBackupFile>) {
        writer.record("settings") {
            val settings = BackupPreferenceCodec(context).export()
            val home = HomePreferences(context)
            home.backgroundPath?.let { path ->
                val file = File(path)
                if (file.isFile) { settings.put("backgroundPath", file.name); files.add(NativeBackupFile(file)) }
            }
            val logo = home.logoContent
            if (home.logoMode == LogoMode.IMAGE && logo != null) {
                val start = logo.indexOf("file://")
                val end = logo.indexOf('"', start)
                if (end > start && start > 0) {
                    val file = File(logo.substring(start + 7, end))
                    if (file.isFile) { settings.put("logoPath", file.name); files.add(NativeBackupFile(file)) }
                }
            }
            settings
        }
    }
}
