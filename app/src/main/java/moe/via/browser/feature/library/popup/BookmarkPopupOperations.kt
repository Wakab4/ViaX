package moe.via.browser.feature.library.popup

import android.content.Context
import android.os.SystemClock
import moe.via.browser.feature.library.LibraryEntry
import moe.via.browser.feature.library.LibraryKind
import moe.via.browser.feature.library.data.LibraryRepository
import moe.via.browser.feature.library.data.BookmarkText
import moe.via.browser.feature.library.forms.BookmarkFolderChoices
import moe.via.browser.feature.search.data.SearchEngineRepository
import moe.via.browser.feature.search.resolve.SearchUrlResolver

/** Work inputs and app-scoped operations never retain a Fragment or a window callback. */
internal object BookmarkPopupOperations {
    sealed interface Outcome {
        data class Loaded(val entry: LibraryEntry, val folder: LibraryEntry) : Outcome
        data class Folders(val rows: List<BookmarkPopupFolderRow>) : Outcome
        data class Saved(val entry: LibraryEntry?) : Outcome
        data class Deleted(val success: Boolean) : Outcome
        data class Created(val folder: LibraryEntry?) : Outcome
    }

    fun load(repository: LibraryRepository, request: BookmarkPopupRequest): Outcome.Loaded {
        val entry = if (request.id != null) repository.findBookmarkById(request.id)
            else request.url?.let(repository::findNativeBookmark)
        val folder = entry?.parentId?.takeIf { it.isNotEmpty() }?.let { repository.findFolder(it, showHidden = true) }
            ?: RecentFolder.get()?.takeIf { it.isNotEmpty() }?.let(repository::findFolder)
            ?: root()
        return Outcome.Loaded(entry ?: LibraryEntry("", LibraryKind.BOOKMARK,
            BookmarkText.clean(request.title).orEmpty(), request.url ?: "https://"), folder)
    }

    fun folders(app: Context, repository: LibraryRepository, selected: String): Outcome.Folders =
        Outcome.Folders(BookmarkFolderChoices.read(app, repository.folders()).map {
            BookmarkPopupFolderRow(it.entry, it.depth, it.entry.id == selected)
        })

    fun search(repository: LibraryRepository, raw: String, selected: String): Outcome.Folders =
        Outcome.Folders(repository.searchFolders(raw).map { BookmarkPopupFolderRow(it, 0, it.id == selected) })

    fun save(app: Context, repository: LibraryRepository, original: LibraryEntry, folder: String,
        rawUrl: String, title: String, favorite: Boolean): Outcome.Saved {
        val template = SearchEngineRepository(app).use { it.selected().mobileTemplate }
        val url = SearchUrlResolver.resolve(rawUrl, template)?.url ?: rawUrl
        // b8.y.m deliberately writes the optional favorite before the bookmark operation.
        if (favorite) repository.saveFavorite(title, url)
        val saved = repository.saveBookmarkEntry(original, title, url, folder)
        if (saved == null) return Outcome.Saved(null)
        if (original.id.isEmpty()) RecentFolder.put(folder)
        return Outcome.Saved(saved)
    }

    fun delete(repository: LibraryRepository, id: String): Outcome.Deleted =
        Outcome.Deleted(repository.deleteBookmarkById(id))

    fun createFolder(repository: LibraryRepository, title: String, parent: String): Outcome.Created {
        return Outcome.Created(repository.createFolderEntry(title, parent,
            nowSeconds = System.currentTimeMillis() / 1000, normalizeTitle = false))
    }

    private fun root() = LibraryEntry("", LibraryKind.FOLDER, "")

    /** v5.b's editbookmarkcache is process-only; an equal expiry second remains valid. */
    private object RecentFolder {
        private var id: String? = null
        private var expires = 0L
        @Synchronized fun get(): String? {
            if (expires < SystemClock.elapsedRealtime() / 1000) { id = null; return null }
            return id
        }
        @Synchronized fun put(folder: String) { id = folder; expires = SystemClock.elapsedRealtime() / 1000 + 180 }
    }
}
