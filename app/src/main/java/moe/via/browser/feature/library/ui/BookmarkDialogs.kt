package moe.via.browser.feature.library.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import moe.via.browser.feature.library.forms.BookmarkFormNavigation
import moe.via.browser.ui.tasks.NativeViewJobs
import moe.via.browser.feature.library.LibraryEntry
import moe.via.browser.feature.library.data.LibraryRepository

/** The actual caller owns the three native result listeners and the move operation. */
class BookmarkDialogs(private val owner: Fragment, private val repository: LibraryRepository,
    private val listener: LifecycleOwner = owner) : AutoCloseable {
    private val jobs = NativeViewJobs()
    fun edit(title: String, url: String, folder: String = "", existing: LibraryEntry? = null,
        favoriteEnabled: Boolean = false, showHidden: Boolean = false, onSaved: () -> Unit) = BookmarkFormNavigation.bookmark(owner, listener,
        existing?.id, title, url, folder, favoriteEnabled, showHidden, onSaved)
    fun folder(parent: String, existing: LibraryEntry? = null, showHidden: Boolean = false, onSaved: () -> Unit) =
        BookmarkFormNavigation.folder(owner, listener, existing?.id, parent, existing != null, showHidden) { onSaved() }
    fun move(entries: List<LibraryEntry>, parent: String = entries.firstOrNull()?.parentId.orEmpty(),
        showHidden: Boolean = false, onMoved: () -> Unit) =
        BookmarkFormNavigation.picker(owner, listener, parent, true, showHidden) { target ->
            jobs.launch({ repository.move(entries, target) }) { onMoved() }
        }
    fun dispose() = close()
    override fun close() { jobs.close() }
}
