package moe.via.browser.storage.database.schema

import android.database.sqlite.SQLiteDatabase

object SiteSchema {
    fun create(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE conf(domain TEXT NOT NULL PRIMARY KEY COLLATE BINARY,data TEXT)")
    }
}
