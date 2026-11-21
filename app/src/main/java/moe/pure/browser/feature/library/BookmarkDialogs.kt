package moe.pure.browser.feature.library

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import moe.pure.browser.feature.library.forms.BookmarkFormNavigation
import moe.pure.browser.ui.tasks.NativeViewJobs

/** The actual caller owns the three native result listeners and the move operation. */
class BookmarkDialogs(private val owner: Fragment, private val repository: LibraryRepository,
    private val listener: LifecycleOwner = owner) : AutoCloseable {
    private val jobs = NativeViewJobs()
    fun edit(title: String, url: String, folder: String = "", existing: LibraryEntry? = null,
        favoriteEnabled: Boolean = false, onSaved: () -> Unit) = BookmarkFormNavigation.bookmark(owner, listener,
        existing?.id, title, url, folder, favoriteEnabled, onSaved)
    fun folder(parent: String, existing: LibraryEntry? = null, onSaved: () -> Unit) =
        BookmarkFormNavigation.folder(owner, listener, existing?.id, parent, existing != null) { onSaved() }
    fun move(entries: List<LibraryEntry>, parent: String = entries.firstOrNull()?.parentId.orEmpty(), onMoved: () -> Unit) =
        BookmarkFormNavigation.picker(owner, listener, parent, true) { target ->
            jobs.launch({ repository.move(entries, target) }) { onMoved() }
        }
    fun dispose() = close()
    override fun close() { jobs.close() }
}
