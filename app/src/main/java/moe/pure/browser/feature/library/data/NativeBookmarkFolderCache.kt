package moe.pure.browser.feature.library.data

import android.database.sqlite.SQLiteDatabase
import java.util.concurrent.ConcurrentHashMap

/** o9.e caches folders for the process; items remain DAO reads. No database owner is retained. */
internal class NativeBookmarkFolderCache {
    val values = HashMap<String, NativeBookmarkFolder>()
    var complete = false

    companion object {
        private val owners = ConcurrentHashMap<String, NativeBookmarkFolderCache>()
        fun forDatabase(db: SQLiteDatabase) = owners.getOrPut(db.path) { NativeBookmarkFolderCache() }
    }
}
