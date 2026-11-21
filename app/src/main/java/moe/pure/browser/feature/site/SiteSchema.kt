package moe.pure.browser.feature.site

import android.database.sqlite.SQLiteDatabase

object SiteSchema {
    fun create(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE conf(domain TEXT NOT NULL PRIMARY KEY COLLATE BINARY,data TEXT)")
    }
}
