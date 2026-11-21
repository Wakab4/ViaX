package moe.pure.browser.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import moe.pure.browser.feature.library.LibrarySchema
import moe.pure.browser.browser.TabSchema
import moe.pure.browser.feature.site.SiteSchema
import moe.pure.browser.feature.download.DownloadSchema
import moe.pure.browser.feature.scripts.data.ScriptSchema
import moe.pure.browser.data.settings.NativeSettingsRows
import moe.pure.browser.feature.history.data.HistoryRows
import moe.pure.browser.feature.history.data.HistorySchema
import moe.pure.browser.feature.home.data.NativeFavoriteRows
import moe.pure.browser.feature.library.data.NativeBookmarkRows
import moe.pure.browser.feature.internalpages.GeneratedPageDirtyState

data class SavedPage(val title: String, val url: String)

/** Database owner. Feature repositories own their queries; WebView owns cookies/cache. */
class BrowserStore(context: Context, databaseName: String = "browser.db") : SQLiteOpenHelper(context, databaseName, null, VERSION) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(db.version == VERSION) }
    override fun onOpen(db: SQLiteDatabase) { super.onOpen(db); db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        LibrarySchema.create(db)
        NativeFavoriteRows.create(db)
        HistorySchema.create(db)
        NativeSettingsRows.create(db)
        TabSchema.create(db)
        SiteSchema.create(db)
        DownloadSchema.create(db)
        ScriptSchema.create(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // The development prototype has no supported old-data format to migrate.
        val tables = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' " +
            "AND name NOT LIKE 'sqlite_%' AND name<>'android_metadata'", null).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        tables.forEach { name -> db.execSQL("DROP TABLE \"${name.replace("\"", "\"\"")}\"") }
        onCreate(db)
    }

    fun saveBookmark(title: String, url: String) {
        if (NativeBookmarkRows(writableDatabase).addItem(title, url, "", System.currentTimeMillis() / 1000).isNotEmpty())
            GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.BOOKMARKS)
    }

    fun removeBookmark(url: String) {
        if (NativeBookmarkRows(writableDatabase).deleteUrl(url) > 0)
            GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.BOOKMARKS)
    }

    fun isBookmarked(url: String): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM bookmark_items WHERE url = ? LIMIT 1", arrayOf(url)
    ).use { it.moveToFirst() }

    fun bookmarks(): List<SavedPage> = readPages("SELECT title, url FROM bookmark_items ORDER BY created_at DESC")

    fun recordVisit(title: String, url: String) {
        HistoryRows(writableDatabase).recordNative(url, title)
    }

    fun history(): List<SavedPage> = HistoryRows(readableDatabase).read(limit = "500").rows.map { SavedPage(it.title.orEmpty(), it.url.orEmpty()) }

    fun removeHistory(url: String) {
        HistoryRows(writableDatabase).deleteUrl(url)
    }

    fun clearHistory() {
        HistoryRows(writableDatabase).clear()
    }

    private fun readPages(sql: String): List<SavedPage> = buildList {
        readableDatabase.rawQuery(sql, null).use { cursor ->
            while (cursor.moveToNext()) add(SavedPage(cursor.getString(0).orEmpty(), cursor.getString(1).orEmpty()))
        }
    }
    companion object { const val VERSION = 27 }
}
