package moe.pure.browser.feature.download

import android.database.sqlite.SQLiteDatabase

object DownloadSchema {
    fun create(db: SQLiteDatabase) {
        DownloadPayloadSchema.create(db)
        db.execSQL("""CREATE TABLE download_tasks (
            _id INTEGER PRIMARY KEY AUTOINCREMENT,url TEXT NOT NULL,name TEXT NOT NULL,target TEXT NOT NULL,
            mime TEXT NOT NULL,headers TEXT NOT NULL,state TEXT NOT NULL,total INTEGER NOT NULL DEFAULT -1,
            received INTEGER NOT NULL DEFAULT 0,etag TEXT,last_modified TEXT,connections INTEGER NOT NULL DEFAULT 1,
            error TEXT,created INTEGER NOT NULL,updated INTEGER NOT NULL,finished INTEGER NOT NULL DEFAULT 0,
            payload TEXT REFERENCES download_payloads(token),transfer_flags INTEGER NOT NULL DEFAULT 1)""")
        db.execSQL("CREATE INDEX downloads_recent ON download_tasks(created DESC,_id DESC)")
        db.execSQL("""CREATE TABLE download_parts (
            task_id INTEGER NOT NULL REFERENCES download_tasks(_id) ON DELETE CASCADE,
            part_index INTEGER NOT NULL,start_byte INTEGER NOT NULL,end_byte INTEGER NOT NULL,
            received INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(task_id,part_index))""")
    }
}
