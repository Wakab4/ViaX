package moe.via.browser.feature.library.bookmarks.document

import moe.via.browser.core.logging.NativeLog as Log
import android.os.Handler
import android.os.Looper
import moe.via.browser.R
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.home.data.NativeHomeFavorites
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import moe.via.browser.feature.library.ui.BookmarkDialogs
import moe.via.browser.browser.ui.host.BrowserRootFragment
import moe.via.browser.feature.library.data.LibraryRepository
import moe.via.browser.feature.library.popup.BookmarkPopupController
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.theme.ThemedActivity
import java.lang.ref.WeakReference

/** Z5 actions 9–13 resolve bookmark variants, but deletion retains exact URL identity. */
class BookmarkDocumentActions(private val activity: ThemedActivity, private val refreshCurrent: () -> Unit,
    private val popup: BookmarkPopupController) {
    private val repositoryDelegate = lazy { LibraryRepository(activity.applicationContext) }
    private val repository by repositoryDelegate
    private val dialogsDelegate = lazy { BookmarkDialogs(requireNotNull(BrowserRootFragment.find(activity)), repository) }
    private val dialogs by dialogsDelegate
    private val message = BrowserMessage(activity)
    private val main = Handler(Looper.getMainLooper())
    private var disposed = false

    fun edit(url: String) {
        if (disposed) return
        if (moe.via.browser.feature.internalpages.BrowserInternalPages.kind(activity, url) > 0) {
            popup.show("https://", "")
            return
        }
        val app = activity.applicationContext
        val owner = WeakReference(this)
        val dispatcher = main
        BrowserExecutors.io.execute {
            val result = runCatching { LibraryRepository(app).use { it.findNativeBookmark(url) } }
            dispatcher.post {
                val controller = owner.get()?.takeUnless { it.disposed } ?: return@post
                result.onSuccess { entry ->
                    if (entry == null) controller.popup.show(url)
                    else controller.popup.show(id = entry.id)
                }
                    .onFailure { Log.warn("BookmarkDocument", "Read bookmark for editor", it) }
            }
        }
    }

    fun favorite(url: String) = perform {
        val entry = repository.findNativeBookmark(url) ?: return@perform
        if (NativeHomeFavorites.get(activity).save(entry.title, entry.url) > 0) {
            GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.HOME)
            message.show(activity.getString(R.string.ref_bookmark_added_home))
        }
    }

    fun delete(url: String) = perform {
        if (url.isEmpty()) return@perform
        val removed = repository.deleteBookmarkUrl(url)
        if (removed > 0) changed()
    }

    fun editFolder(folder: String) = perform {
        val entry = repository.findFolder(folder) ?: return@perform
        dialogs.folder(entry.parentId, entry) { changed() }
    }

    /** The context owner has already shown Native's confirmation with the resolved folder key. */
    fun deleteFolder(folder: String) = perform {
        if (folder.isEmpty()) return@perform
        val entry = repository.findFolder(folder)?.takeUnless { it.id.isEmpty() } ?: return@perform
        repository.delete(listOf(entry))
        changed()
    }

    private fun changed() {
        if (disposed) return
        GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.BOOKMARKS)
        refreshCurrent()
    }
    private inline fun perform(action: () -> Unit) {
        if (disposed) return
        try { action() } catch (error: Exception) { Log.warn("BookmarkDocument", "Bookmark action", error) }
    }
    fun dispose() {
        disposed = true
        if (dialogsDelegate.isInitialized()) dialogs.close()
        if (repositoryDelegate.isInitialized()) repository.close()
        message.close()
    }
}
