package moe.via.browser.storage.database.schema

import android.database.sqlite.SQLiteDatabase

object LibrarySchema {
    fun create(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE bookmark_items(_id TEXT PRIMARY KEY,url TEXT,title TEXT,folder_id TEXT,ordering INTEGER,last_updated_at INTEGER DEFAULT 0,created_at INTEGER DEFAULT 0)")
        db.execSQL("CREATE TABLE bookmark_folders(_id TEXT PRIMARY KEY,title TEXT,parent_folder_id TEXT,ordering INTEGER,created_at INTEGER DEFAULT 0,last_updated_at INTEGER DEFAULT 0)")
    }
}
