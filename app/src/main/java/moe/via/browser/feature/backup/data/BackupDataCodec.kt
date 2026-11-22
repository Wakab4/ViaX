package moe.via.browser.feature.backup.data

import android.content.Context
import moe.via.browser.storage.database.BrowserDatabase
import moe.via.browser.browser.session.data.TabEntry
import moe.via.browser.browser.session.data.TabRepository
import moe.via.browser.feature.backup.model.ImportMode
import moe.via.browser.feature.backup.model.SettingsImportKind
import moe.via.browser.feature.backup.model.BackupBookmarkRow
import moe.via.browser.feature.backup.archive.NativeBackupDirectory
import moe.via.browser.feature.backup.data.export.FavoritesFileExport
import moe.via.browser.feature.backup.data.export.ClosedTabsFileExport
import org.json.JSONObject
import java.io.Closeable
import java.util.UUID

/** Worker-only codecs shared by local ZIP backup and the three-file sync protocol. */
class BackupDataCodec(context: Context) : Closeable {
    private val app = context.applicationContext
    private val store = BrowserDatabase(app)
    private val db get() = store.writableDatabase
    fun exportBookmarks(): String? = BookmarkBackupCodec(db).export()
    internal fun exportBookmarkFile(file: java.io.File) = BookmarkBackupCodec(db).exportFile(file)
    internal fun exportFavoriteFiles(directory: java.io.File) = FavoritesFileExport(app, db).write(directory)
    internal fun exportHistoryFile(file: java.io.File) = HistoryBackupCodec(app, db).exportFile(file)
    internal fun exportClosedTabsFile(file: java.io.File) = ClosedTabsFileExport(app, db).write(file)
    fun importBookmarks(text: String, mode: ImportMode = ImportMode.MERGE, baselineMillis: Long = 0): Int =
        BookmarkBackupCodec(db).restore(text, mode, baselineMillis)
    fun exportBookmarkRows(): List<BackupBookmarkRow> = BookmarkBackupCodec(db).rows()
    fun importBookmarkRows(rows: List<BackupBookmarkRow>, mode: ImportMode = ImportMode.MERGE, baselineMillis: Long = 0): Int =
        BookmarkBackupCodec(db).restoreRows(rows, mode, baselineMillis)
    fun exportFavorites(): String? = FavoriteBackupCodec(app, db).export()
    fun importFavorites(text: String, mode: ImportMode = ImportMode.MERGE): Int = FavoriteBackupCodec(app, db).restore(text, mode)
    internal fun restoreFavoriteFile(file: java.io.File, icons: java.io.File?, mode: ImportMode = ImportMode.MERGE): Int =
        FavoriteBackupCodec(app, db).restore(file, icons, mode)
    fun exportHistory(): String? = HistoryBackupCodec(app, db).export()
    fun importHistory(text: String, mode: ImportMode = ImportMode.MERGE): Int = HistoryBackupCodec(app, db).restore(text, mode)
    fun exportClosedTabs(): String? = TabRepository(app, db).use { repository ->
        repository.exportClosed().takeIf { it.isNotEmpty() }?.joinToString(separator = "\n", postfix = "\n") { entry ->
            JSONObject().put("title", entry.title).put("url", entry.url)
                .put("updated_at", entry.lastVisitedAt).toString()
        }
    }
    fun importClosedTabs(text: String): Int {
        val entries = jsonLines(text).mapNotNull { item ->
            val url = item.optString("url").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            TabEntry(id = UUID.randomUUID().toString(), url = url, title = item.optString("title"),
                filePath = null, flags = 0, lastVisitedAt = item.optLong("updated_at"),
                home = moe.via.browser.feature.home.HomeDocument.isHome(app, url))
        }
        return TabRepository(app, db).use { it.importClosed(entries) }
    }
    fun exportSettings(): String? {
        val files = BackupSettingsCodec(app, db).export(NativeBackupDirectory.create(app), inline = true)
        return files.firstOrNull()?.file?.readText(Charsets.UTF_8)
    }
    fun exportSettingValues(compact: Boolean = false): JSONObject = BackupPreferenceCodec(app).export(compact)
    fun importSettings(text: String, mode: ImportMode = ImportMode.MERGE, kind: SettingsImportKind = SettingsImportKind.CURRENT_LINES): Int =
        BackupSettingsCodec(app, db).restore(text, kind = kind)
    internal fun exportSettingsFiles(directory: java.io.File) = BackupSettingsCodec(app, db).export(directory, inline = false)
    internal fun restoreSettings(file: java.io.File, attachments: Map<String, java.io.File>, kind: SettingsImportKind = SettingsImportKind.CURRENT_LINES): Int =
        BackupSettingsCodec(app, db).restore(file, attachments, kind)
    private fun jsonLines(text: String) = text.lineSequence().filter(String::isNotBlank).mapNotNull { runCatching { JSONObject(it) }.getOrNull() }.toList()
    private fun millis(seconds: Long) = seconds.coerceIn(0, Long.MAX_VALUE / 1000) * 1000
    override fun close() = store.close()
}
