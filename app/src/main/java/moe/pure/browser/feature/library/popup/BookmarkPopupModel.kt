package moe.pure.browser.feature.library.popup

import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import moe.pure.browser.core.logging.NativeLog as Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.library.LibraryRepository
import java.lang.ref.WeakReference
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicReference

/** b8.y owns real Futures and draft values across view replacement; construction performs no IO. */
class BookmarkPopupModel(application: Application) : AndroidViewModel(application) {
    private val app = application.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val updates = MutableLiveData(BookmarkPopupState())
    val state: LiveData<BookmarkPopupState> = updates
    private var value = BookmarkPopupState()
    private var initialized = false
    val isInitialized: Boolean get() = initialized
    private var cleared = false
    private var request = BookmarkPopupRequest()
    private var draftEdited = false
    private var lastSubmit = 0L
    private var nextTask = 0L
    private var nextEvent = 0L
    private val selected = AtomicReference("")
    private val tasks = mutableMapOf<Long, FutureTask<Unit>>()
    private val events = ArrayDeque<BookmarkPopupEvent>()

    fun initialize(request: BookmarkPopupRequest, savedState: Bundle? = null) {
        if (initialized || cleared) return
        initialized = true
        this.request = request
        draftEdited = savedState?.getBoolean("draft", false) == true
        publish(value.copy(draftTitle = savedState?.getString("title") ?: request.title.orEmpty(),
            draftUrl = savedState?.getString("url") ?: request.url ?: "https://",
            favorite = savedState?.getBoolean("favorite") ?: false,
            query = savedState?.getString("query").orEmpty(),
            page = (savedState?.getInt("page") ?: 0).coerceIn(0, 1),
            height = savedState?.getInt("height") ?: 0))
        submit { repository -> BookmarkPopupOperations.load(repository, request) }
    }

    fun setDraft(title: String, url: String, favorite: Boolean) {
        if (cleared) return
        if (title == value.draftTitle && url == value.draftUrl && favorite == value.favorite) return
        draftEdited = true
        publish(value.copy(draftTitle = title, draftUrl = url, favorite = favorite))
    }

    fun showFolders() {
        if (!value.loaded || cleared) return
        page(1)
        loadFolders()
    }
    fun page(index: Int) { if (!cleared && index in 0..1) publish(value.copy(page = index)) }
    fun setHeight(px: Int) { if (!cleared && px > 0 && px != value.height) publish(value.copy(height = px)) }

    fun search(raw: String) {
        if (!value.loaded || cleared) return
        publish(value.copy(query = raw))
        if (raw.isEmpty()) loadFolders()
        else {
            val selection = selected
            submit { repository -> BookmarkPopupOperations.search(repository, raw, selection.get()) }
        }
    }

    fun chooseFolder(id: String) {
        if (cleared) return
        val folder = value.folderRows.firstOrNull { it.entry.id == id }?.entry
        if (folder != null) selected.set(folder.id)
        publish(value.copy(currentFolder = folder ?: value.currentFolder,
            folderRows = value.folderRows.map { it.copy(selected = it.entry.id == id) }, page = 0))
    }

    fun createFolder(raw: String) {
        if (cleared || !value.loaded || raw.isEmpty()) return
        val parent = value.currentFolder?.id.orEmpty()
        submit { repository -> BookmarkPopupOperations.createFolder(repository, raw, parent) }
    }

    fun save(rawUrl: String, title: String, favorite: Boolean) {
        val entry = value.entry ?: return
        if (!acceptSubmit()) return
        val folder = value.currentFolder?.id.orEmpty()
        val application = app
        submit { repository -> BookmarkPopupOperations.save(application, repository, entry, folder, rawUrl, title, favorite) }
    }

    fun delete() {
        val entry = value.entry ?: return
        if (!acceptSubmit()) return
        submit { repository -> BookmarkPopupOperations.delete(repository, entry.id) }
    }
    fun cancel() { if (!cleared) event(BookmarkPopupEventKind.CLOSE) }

    fun consumeEvent(id: Long) {
        if (value.event?.id != id) return
        if (events.firstOrNull()?.id == id) events.removeFirst()
        publish(value.copy(event = events.firstOrNull()))
    }

    /** Only view values survive a new process; selected-folder/cache and in-flight writes do not. */
    fun saveState(outState: Bundle) {
        outState.putBundle("request", request.toBundle())
        outState.putBoolean("draft", true)
        outState.putString("title", value.draftTitle); outState.putString("url", value.draftUrl)
        outState.putBoolean("favorite", value.favorite); outState.putString("query", value.query)
        outState.putInt("page", value.page); outState.putInt("height", value.height)
    }

    private fun acceptSubmit(): Boolean {
        if (cleared || !value.loaded || SystemClock.elapsedRealtime() - lastSubmit < 300) return false
        lastSubmit = SystemClock.elapsedRealtime()
        return true
    }

    private fun loadFolders() {
        val application = app
        val selection = selected
        submit { repository -> BookmarkPopupOperations.folders(application, repository, selection.get()) }
    }

    private fun submit(work: (LibraryRepository) -> BookmarkPopupOperations.Outcome) {
        if (cleared) return
        val id = ++nextTask
        val application = app
        val owner = WeakReference(this)
        val dispatcher = main
        val task = FutureTask<Unit>(Callable {
            val result = runCatching { LibraryRepository(application).use(work) }
            dispatcher.post { owner.get()?.takeUnless { it.cleared }?.completed(id, result) }
            Unit
        })
        tasks[id] = task
        publish(value)
        BrowserExecutors.io.execute(task)
    }

    private fun completed(id: Long, result: Result<BookmarkPopupOperations.Outcome>) {
        tasks.remove(id)
        result.fold({ outcome ->
            when (outcome) {
                is BookmarkPopupOperations.Outcome.Loaded -> {
                    selected.set(outcome.folder.id)
                    publish(value.copy(entry = outcome.entry, currentFolder = outcome.folder, loaded = true,
                        draftTitle = if (draftEdited) value.draftTitle else outcome.entry.title,
                        draftUrl = if (draftEdited) value.draftUrl else outcome.entry.url))
                    if (value.page == 1) search(value.query)
                }
                is BookmarkPopupOperations.Outcome.Folders -> publish(value.copy(folderRows = outcome.rows))
                is BookmarkPopupOperations.Outcome.Saved -> {
                    if (outcome.entry == null) failed(null)
                    else { publish(value.copy(entry = outcome.entry)); event(BookmarkPopupEventKind.SAVED) }
                }
                is BookmarkPopupOperations.Outcome.Deleted -> if (outcome.success) event(BookmarkPopupEventKind.DELETED) else failed(null)
                is BookmarkPopupOperations.Outcome.Created -> {
                    if (outcome.folder == null) failed(null)
                    else {
                        selected.set(outcome.folder.id)
                        publish(value.copy(currentFolder = outcome.folder, page = 0))
                        loadFolders()
                    }
                }
            }
        }, ::failed)
    }

    private fun failed(error: Throwable?) {
        if (error != null) Log.warn("BookmarkPopup", "Bookmark operation", error)
        event(BookmarkPopupEventKind.FAILED)
    }
    private fun event(kind: BookmarkPopupEventKind) {
        val event = BookmarkPopupEvent(++nextEvent, kind, value.entry, request.position)
        events.addLast(event)
        publish(value.copy(event = events.firstOrNull()))
    }
    private fun publish(next: BookmarkPopupState) {
        if (cleared) return
        value = next.copy(busy = tasks.isNotEmpty())
        updates.value = value
    }

    override fun onCleared() {
        cleared = true
        tasks.values.forEach { it.cancel(false) }; tasks.clear(); events.clear()
        super.onCleared()
    }
}
