package moe.via.browser.feature.sync.sync

import android.content.Context
import moe.via.browser.feature.backup.data.BackupDataCodec
import moe.via.browser.feature.backup.model.ImportMode
import moe.via.browser.feature.sync.model.SyncData
import java.io.Closeable

/** Three allowlisted datasets only. Passwords, history, icons and ZIP attachments are not WebDAV files. */
interface SyncDataAdapter : Closeable {
    fun export(data: SyncData): String?
    fun importData(data: SyncData, text: String, mode: ImportMode, baselineMillis: Long = 0): Int
    override fun close() = Unit
}

class BrowserSyncDataAdapter(context: Context) : SyncDataAdapter {
    private val codec = BackupDataCodec(context.applicationContext)
    override fun export(data: SyncData): String? = when (data) {
        SyncData.BOOKMARKS -> codec.exportBookmarks()
        SyncData.FAVORITES -> codec.exportFavorites()
        SyncData.SETTINGS -> codec.exportSettings()
    }
    override fun importData(data: SyncData, text: String, mode: ImportMode, baselineMillis: Long): Int = when (data) {
        SyncData.BOOKMARKS -> codec.importBookmarks(text, mode, baselineMillis)
        SyncData.FAVORITES -> codec.importFavorites(text, mode)
        SyncData.SETTINGS -> codec.importSettings(text, mode)
    }
    override fun close() = codec.close()
}
