package moe.pure.browser.feature.library.forms

import android.content.Context
import moe.pure.browser.feature.library.LibraryEntry
import moe.pure.browser.feature.library.LibraryKind
import moe.pure.browser.feature.library.data.BookmarkOrdering

internal data class BookmarkFolderChoice(val entry: LibraryEntry, val depth: Int)

/** a8.d.b expands the sorted flat table in parent order; orphan/cyclic folders remain unreachable. */
internal object BookmarkFolderChoices {
    fun read(context: Context, folders: List<LibraryEntry>): List<BookmarkFolderChoice> {
        val order = BookmarkOrdering.preferredOrder(context.getSharedPreferences("browser", Context.MODE_PRIVATE))
        val pending = BookmarkOrdering(context).sort(folders.filter { it.id.isNotEmpty() }, order).toMutableList()
        val result = mutableListOf(BookmarkFolderChoice(LibraryEntry("", LibraryKind.FOLDER, ""), 0))
        var position = 0
        while (position < result.size) {
            val parent = result[position++]
            for (index in pending.lastIndex downTo 0) if (pending[index].parentId == parent.entry.id) {
                result.add(position, BookmarkFolderChoice(pending.removeAt(index), parent.depth + 1))
            }
        }
        return result
    }
}
