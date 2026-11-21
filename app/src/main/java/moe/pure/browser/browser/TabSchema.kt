package moe.pure.browser.browser

import android.database.sqlite.SQLiteDatabase

object TabSchema {
    fun create(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE tabs (_id TEXT PRIMARY KEY,url TEXT,title TEXT,file_path TEXT," +
            "flags INTEGER DEFAULT 0,last_visited_at INTEGER DEFAULT 0," +
            "home INTEGER NOT NULL DEFAULT 0,desktop INTEGER NOT NULL DEFAULT 0," +
            "scroll_x INTEGER NOT NULL DEFAULT 0,scroll_y INTEGER NOT NULL DEFAULT 0)")
        db.execSQL("CREATE INDEX tabs_recent ON tabs(last_visited_at DESC)")
    }

}
