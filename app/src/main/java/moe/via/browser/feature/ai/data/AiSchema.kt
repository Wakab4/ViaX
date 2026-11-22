package moe.via.browser.feature.ai.data

import android.database.sqlite.SQLiteDatabase

object AiSchema {
    fun create(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS backends (
            id TEXT PRIMARY KEY NOT NULL,name TEXT NOT NULL,url TEXT NOT NULL,token TEXT NOT NULL,
            models TEXT,flags INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0,created_at INTEGER DEFAULT 0)
        """)
        db.execSQL("""CREATE TABLE IF NOT EXISTS threads (
            id TEXT PRIMARY KEY NOT NULL,name TEXT,preview TEXT,flags INTEGER DEFAULT 0,
            updated_at INTEGER DEFAULT 0,created_at INTEGER DEFAULT 0)
        """)
        db.execSQL("""CREATE TABLE IF NOT EXISTS messages (
            id TEXT PRIMARY KEY NOT NULL,thread_id TEXT NOT NULL,content TEXT NOT NULL,reasoning TEXT,
            role TEXT NOT NULL,flags INTEGER DEFAULT 0,updated_at INTEGER DEFAULT 0)
        """)
        db.execSQL("""CREATE TABLE IF NOT EXISTS suggestions (
            id TEXT PRIMARY KEY NOT NULL,name TEXT,content TEXT,vendors TEXT,required TEXT,
            type INTEGER DEFAULT 0,flags INTEGER DEFAULT 0,
            updated_at INTEGER DEFAULT 0,created_at INTEGER DEFAULT 0)
        """)
    }
}
