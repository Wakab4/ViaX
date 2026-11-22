package moe.via.browser.feature.ai.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** p9.w owns its independent copilot database, separate from browser metadata. */
internal class AiStore(context: Context) : SQLiteOpenHelper(context, NAME, null, VERSION) {
    override fun onCreate(db: SQLiteDatabase) = AiSchema.create(db)
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Development schemas are rebuilt; the prototype has no supported migration format.
        listOf("backends", "threads", "messages", "suggestions").forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }
    companion object { const val NAME = "copilot"; const val VERSION = 4 }
}
