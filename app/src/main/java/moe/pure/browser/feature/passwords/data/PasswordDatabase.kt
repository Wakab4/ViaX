package moe.pure.browser.feature.passwords.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteOpenHelper
import android.database.DatabaseErrorHandler

/** Independent of BrowserStore and its schema versions. Only the password column is encrypted. */
internal class PasswordDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, "passwords.db", null, 1,
    DatabaseErrorHandler { throw SQLiteDatabaseCorruptException("密码数据库损坏，已保留原文件。") }) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE pass (id TEXT PRIMARY KEY NOT NULL,name TEXT,url TEXT,username TEXT,password TEXT,note TEXT,updated_at INTEGER DEFAULT 0,created_at INTEGER DEFAULT 0)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Unsupported password database upgrade: $oldVersion to $newVersion")
    }
}
