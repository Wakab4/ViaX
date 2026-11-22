package moe.via.browser.storage.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import moe.via.browser.storage.database.schema.DownloadSchema
import moe.via.browser.storage.database.schema.FavoritesSchema
import moe.via.browser.storage.database.schema.HistorySchema
import moe.via.browser.storage.database.schema.LibrarySchema
import moe.via.browser.storage.database.schema.ScriptSchema
import moe.via.browser.storage.database.schema.SiteSchema
import moe.via.browser.storage.database.schema.TabSchema
import moe.via.browser.storage.database.settings.NativeSettingsRows

class BrowserDatabase(context: Context, databaseName: String = "browser.db") : SQLiteOpenHelper(context, databaseName, null, VERSION) {
    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(db.version == VERSION) }
    override fun onOpen(db: SQLiteDatabase) { super.onOpen(db); db.setForeignKeyConstraintsEnabled(true) }
    override fun onCreate(db: SQLiteDatabase) {
        LibrarySchema.create(db)
        FavoritesSchema.create(db)
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

    companion object { const val VERSION = 27 }
}
