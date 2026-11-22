package moe.via.browser.storage.database.schema

import android.database.sqlite.SQLiteDatabase

object ScriptSchema {
    fun create(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE scripts (_id INTEGER PRIMARY KEY,script_id TEXT NOT NULL,name TEXT NOT NULL,
            version TEXT,content TEXT NOT NULL,matches TEXT,exclude_matches TEXT,includes TEXT,excludes TEXT,
            run_at INTEGER NOT NULL DEFAULT 4,requires TEXT,resources TEXT,enabled INTEGER NOT NULL DEFAULT 1,
            flags INTEGER NOT NULL DEFAULT 0,grant INTEGER NOT NULL DEFAULT 0,icon_url TEXT,homepage_url TEXT,
            support_url TEXT,download_url TEXT,user_overrides TEXT,last_updated_at INTEGER NOT NULL,created_at INTEGER NOT NULL)""")
        db.execSQL("CREATE TABLE script_values(script_id TEXT NOT NULL,name TEXT NOT NULL,value TEXT NOT NULL,PRIMARY KEY(script_id,name))")
    }

}

