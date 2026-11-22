package moe.via.browser.storage.database.schema

import android.database.sqlite.SQLiteDatabase

object DownloadPayloadSchema {
    fun create(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE download_payloads (token TEXT PRIMARY KEY,mime TEXT NOT NULL,size INTEGER NOT NULL DEFAULT 0,state TEXT NOT NULL,created INTEGER NOT NULL,format TEXT NOT NULL DEFAULT 'BINARY')")
    }
}
