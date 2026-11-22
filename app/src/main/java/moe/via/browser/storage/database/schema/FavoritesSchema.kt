package moe.via.browser.storage.database.schema

import android.database.sqlite.SQLiteDatabase

internal object FavoritesSchema {
    fun create(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE favorites(f_id INTEGER PRIMARY KEY,f_url TEXT,f_title TEXT,f_order INTEGER,updated_at INTEGER DEFAULT 0,created_at INTEGER DEFAULT 0)")
    }
}
