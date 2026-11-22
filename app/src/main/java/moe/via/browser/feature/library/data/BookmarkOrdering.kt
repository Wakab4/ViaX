package moe.via.browser.feature.library.data

import android.content.Context
import android.content.SharedPreferences
import moe.via.browser.feature.library.LibraryEntry

/** Shared cb.b order for the compact selector and full manager: folders first, then Native position/name. */
internal class BookmarkOrdering(context: Context) {
    private val app = context.applicationContext
    private val names by lazy { BookmarkTitleComparator(app) }
    fun sort(entries: List<LibraryEntry>, order: Int): List<LibraryEntry> = entries.sortedWith { left, right ->
        if (left.isFolder != right.isFolder) if (left.isFolder) -1 else 1
        else when (order) {
            1 -> right.position.compareTo(left.position)
            2 -> names.compare(left.title, right.title)
            else -> left.position.compareTo(right.position)
        }
    }
    companion object {
        fun preferredOrder(values: SharedPreferences): Int {
            val key = if (values.contains("bookmarksorder")) "bookmarksorder" else "bookmark_sort"
            return try { values.getInt(key, 0) } catch (_: ClassCastException) {
                try { values.getLong(key, 0).toInt() } catch (_: ClassCastException) { 0 }
            }
        }
        fun setOrder(values: SharedPreferences, order: Int) {
            val selected = order.coerceIn(0, 2)
            values.edit().putInt("bookmarksorder", selected).putInt("bookmark_sort", selected).apply()
        }
    }
}
