package moe.via.browser.feature.library.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase

internal class BookmarkFolderVisibility(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("bookmark_visibility", Context.MODE_PRIVATE)

    fun explicitIds(): Set<String> = preferences.getStringSet(HIDDEN_FOLDERS, emptySet()).orEmpty()

    fun excludedIds(db: SQLiteDatabase): Set<String> = excludedIds(NativeBookmarkRows(db))

    fun excludedIds(rows: NativeBookmarkRows): Set<String> {
        val hidden = explicitIds()
        if (hidden.isEmpty()) return hidden
        val children = rows.folders().groupBy(NativeBookmarkFolder::parentId)
        val excluded = hidden.toMutableSet()
        val queue = ArrayDeque(hidden)
        while (queue.isNotEmpty()) {
            for (child in children[queue.removeFirst()].orEmpty()) {
                if (excluded.add(child.id)) queue.addLast(child.id)
            }
        }
        return excluded
    }

    fun setHidden(id: String, hidden: Boolean): Boolean = synchronized(lock) {
        val current = explicitIds()
        val updated = if (hidden) current + id else current - id
        update(current, updated)
    }

    fun forget(ids: Set<String>) = synchronized(lock) {
        val current = explicitIds()
        update(current, current - ids)
    }

    private fun update(current: Set<String>, updated: Set<String>): Boolean {
        if (current == updated) return false
        preferences.edit().putStringSet(HIDDEN_FOLDERS, updated).apply()
        return true
    }

    private companion object {
        const val HIDDEN_FOLDERS = "hidden_folders"
        val lock = Any()
    }
}
