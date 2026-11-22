package moe.via.browser.storage.database.schema

import android.database.sqlite.SQLiteDatabase

/** ga.b.a history fields use seconds and physical row IDs. */
object HistorySchema {
    fun create(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE history(id INTEGER PRIMARY KEY,url TEXT,title TEXT," +
            "updated_at INTEGER DEFAULT 0)")
    }
}
