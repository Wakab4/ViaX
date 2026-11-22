package moe.via.browser.feature.library.forms

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import moe.via.browser.feature.library.data.LibraryRepository
import moe.via.browser.feature.search.data.SearchEngineRepository
import moe.via.browser.feature.search.resolve.SearchUrlResolver
import moe.via.browser.ui.tasks.NativeViewJobs

/** A real form Fragment owns this model; no Activity tokens or saved draft are replayed. */
internal class BookmarkFormModel(context: Context, val request: BookmarkFormRequest) : ViewModel() {
    private val app = context.applicationContext
    private val jobs = NativeViewJobs()
    val updates = MutableLiveData<Unit>()
    var ready = false; private set
    var title = request.title
    var url = request.url.orEmpty()
    var selected = request.folder
    var favorite = false
    var expanded = false
    var folderCreated = false
    var editingId: String? = request.id?.takeIf(String::isNotEmpty); private set
    var isNew = request.id.isNullOrEmpty(); private set
    var folders = emptyList<BookmarkFolderChoice>(); private set
    var result: String? = null; private set
    private var lastSubmit = 0L
    init { load() }
    private fun changed() { updates.value = Unit }
    private fun <T> submit(work: (LibraryRepository) -> T, completed: (T) -> Unit) {
        jobs.launch({ LibraryRepository(app).use(work) }, delivered = completed)
    }
    private fun load() {
        submit({ repository ->
            val entry = request.id?.takeIf(String::isNotEmpty)?.let {
                if (request.kind == BookmarkFormKind.FOLDER) repository.findFolder(it) else repository.findBookmarkById(it)
            }
            Triple(entry, repository.findFolder(entry?.parentId ?: selected), BookmarkFolderChoices.read(app, repository.folders()))
        }) { (entry, parent, choices) ->
            editingId = entry?.id; isNew = entry == null
            if (entry != null) { title = entry.title; url = entry.url }
            selected = parent?.id.orEmpty(); folders = choices; ready = true; changed()
        }
    }
    fun select(id: String) {
        selected = id; changed()
        submit({ BookmarkFolderChoices.read(app, it.folders()) }) { folders = it; changed() }
    }
    fun child(id: String) {
        folderCreated = true; selected = id
        submit({ BookmarkFolderChoices.read(app, it.folders()) }) { folders = it; changed() }
    }
    fun save() {
        if (!ready || SystemClock.elapsedRealtime() - lastSubmit < 300) return
        lastSubmit = SystemClock.elapsedRealtime()
        if (request.kind == BookmarkFormKind.MOVE) { result = selected; changed(); return }
        val name = title
        val address = url.trim { it <= ' ' }
        val addFavorite = request.favoriteEnabled && favorite
        submit({ repository ->
            if (request.kind == BookmarkFormKind.BOOKMARK) {
                val template = SearchEngineRepository(app).use { it.selected().mobileTemplate }
                val normalized = SearchUrlResolver.resolve(address, template)?.url ?: address
                if (addFavorite) repository.saveFavorite(name, normalized)
                repository.saveBookmark(name, normalized, selected, editingId)
            } else repository.saveFolder(name, selected, editingId, nowSeconds = System.currentTimeMillis() / 100)
        }) { id -> if (id.isNotEmpty()) { result = id; editingId = id; changed() } }
    }
    override fun onCleared() { jobs.close() }
}
