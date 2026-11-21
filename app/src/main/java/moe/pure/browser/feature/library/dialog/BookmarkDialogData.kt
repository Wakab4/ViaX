package moe.pure.browser.feature.library.dialog

import android.content.Context
import android.os.Handler
import android.os.Looper
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.library.BookmarkSort
import moe.pure.browser.feature.library.LibraryEntry
import moe.pure.browser.feature.library.LibraryKind
import moe.pure.browser.feature.library.LibraryRepository
import moe.pure.browser.feature.library.data.BookmarkOrdering

internal data class BookmarkDialogRow(val entry: LibraryEntry, val parent: Boolean = false)
internal data class BookmarkDialogFolder(val id: String, val title: String, val rows: List<BookmarkDialogRow>)

/** Owns its connection independently from the Activity's shared repositories. All queries remain on one worker. */
internal class BookmarkDialogData(context: Context, private val injected: LibraryRepository?) : AutoCloseable {
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var repository: LibraryRepository? = injected
    private var generation = 0L
    private var closed = false
    fun load(folderId: String, completed: (Result<BookmarkDialogFolder>) -> Unit) {
        if (closed) return
        val request = ++generation
        BrowserExecutors.persistence.execute {
            val result = runCatching {
                val store = repository ?: LibraryRepository(app).also { repository = it }
                val folders = store.folders()
                val folder = folders.firstOrNull { it.id == folderId } ?: folders.firstOrNull { it.id.isEmpty() }
                val id = folder?.id.orEmpty()
                val values = app.getSharedPreferences("browser", Context.MODE_PRIVATE)
                val order = BookmarkOrdering.preferredOrder(values)
                val rows = store.bookmarks(id, sort = BookmarkSort.entries[order]).map { BookmarkDialogRow(it) }.toMutableList()
                if (id.isNotEmpty()) {
                    val parentId = folder?.parentId.orEmpty()
                    val parent = folders.firstOrNull { it.id == parentId } ?: LibraryEntry("", LibraryKind.FOLDER, "书签")
                    rows.add(0, BookmarkDialogRow(parent.copy(title = ".."), parent = true))
                }
                BookmarkDialogFolder(id, if (id.isEmpty()) "书签" else folder?.title?.takeIf(String::isNotEmpty) ?: "无标题", rows)
            }
            main.post { if (!closed && request == generation) completed(result) }
        }
    }
    fun cancel() { generation++ }
    override fun close() {
        if (closed) return
        closed = true; generation++
        BrowserExecutors.persistence.execute { if (injected == null) repository?.close(); repository = null }
    }
}
