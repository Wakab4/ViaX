package moe.via.browser.feature.library.pages

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import moe.via.browser.feature.library.ui.BookmarkDialogs
import moe.via.browser.feature.library.BookmarkSort
import moe.via.browser.feature.library.LibraryEntry as Entry
import moe.via.browser.feature.library.LibraryKind
import moe.via.browser.feature.library.ui.LibraryRow
import moe.via.browser.feature.library.LibraryTab
import moe.via.browser.feature.library.data.BookmarkDisplayMode
import moe.via.browser.feature.library.data.BookmarkOrdering
import moe.via.browser.feature.library.ui.BookmarkMoreMenuController
import moe.via.browser.feature.library.ui.recycler.LibraryDragController
import moe.via.browser.feature.library.ui.recycler.LibraryProcessState

class BookmarksPageFragment : LibraryItemsPageFragment() {
    override val tab = LibraryTab.BOOKMARKS
    private val preferences by lazy { requireContext().getSharedPreferences("browser", android.content.Context.MODE_PRIVATE) }
    private var folder = ""
    private var folderParent = ""
    private var forms: BookmarkDialogs? = null
    private var more: BookmarkMoreMenuController? = null
    private var drag: LibraryDragController? = null
    private val documents = BookmarkPageDocuments(this, { repository }, ::reload)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState); folder = LibraryProcessState.read(tab)?.folder.orEmpty()
        forms = BookmarkDialogs(parent, repository, viewLifecycleOwner); documents.bind()
        adapter.details = BookmarkDisplayMode.details(preferences)
        more = BookmarkMoreMenuController(requireActivity(), object : BookmarkMoreMenuController.Host {
            override fun state() = BookmarkMoreMenuController.State(folder.isEmpty(), adapter.details, BookmarkOrdering.preferredOrder(preferences))
            override fun addBookmark() { forms?.edit("", "", folder, onSaved = ::reload) }
            override fun addFolder() { forms?.folder(folder, onSaved = ::reload) }
            override fun setOrder(order: Int) {
                if (BookmarkOrdering.preferredOrder(preferences) == order) return
                BookmarkOrdering.setOrder(preferences, order); GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.BOOKMARKS); reload()
            }
            override fun setDetails(details: Boolean) { adapter.details = details; BookmarkDisplayMode.setDetails(preferences, details); update() }
            override fun importBookmarks() = documents.import()
            override fun exportBookmarks() = documents.export()
        })
        drag = LibraryDragController(page!!.list, adapter, object : LibraryDragController.Actions {
            override fun canStart() = query.isEmpty()
            override fun enabled() = query.isEmpty() && BookmarkOrdering.preferredOrder(preferences) != 2
            override fun persist(folder: Boolean) {
                val ordered = adapter.rows.mapNotNull { (it as? LibraryRow.Item)?.takeUnless(LibraryRow.Item::immutable)?.entry }
                entries = ordered
                val group = ordered.filter { it.isFolder == folder }.let { if (BookmarkOrdering.preferredOrder(preferences) == 1) it.reversed() else it }
                work({ repository.reorder(group) }) { }
            }
        })
        reload()
    }
    override fun reload() {
        val currentFolder = folder; val currentQuery = query; val sort = BookmarkSort.entries[BookmarkOrdering.preferredOrder(preferences)]
        work({ val directory = repository.findFolder(currentFolder); directory to repository.bookmarks(directory?.id.orEmpty(), currentQuery.trim(), sort, includeFolderCounts = true) }) { (directory, rows) ->
            folder = directory?.id.orEmpty(); folderParent = directory?.parentId.orEmpty()
            adapter.draggingAllowed = currentQuery.isEmpty() && sort != BookmarkSort.NAME
            val up = directory?.takeIf { it.id.isNotEmpty() && currentQuery.isBlank() }?.let { Entry(it.parentId, LibraryKind.FOLDER, "..") }
            show(rows, up)
        }
    }
    override fun openFolder(entry: Entry) { range.finish(); folder = entry.id; setQueryText(""); reload() }
    override fun editEntry(entry: Entry) {
        if (entry.isFolder) forms?.folder(entry.parentId, entry, ::reload) else forms?.edit(entry.title, entry.url, entry.parentId, entry, onSaved = ::reload)
    }
    override fun drag(holder: RecyclerView.ViewHolder) { drag?.start(holder) }
    override fun footerLeft() { if (editing) super.footerLeft() else page?.bookmarkMoreAnchor?.let { more?.show(it) } }
    override fun footerMove() { forms?.move(adapter.selectedEntries(), folder, ::reload) }
    override fun folderState() = folder
    internal override fun canSwipeBack() = folder.isEmpty() && super.canSwipeBack()
    internal override fun back(): Boolean { if (super.back()) return true; if (folder.isEmpty()) return false; folder = folderParent; reload(); return true }
    override fun onDestroyView() { drag?.close(); drag = null; more?.close(); more = null; forms?.close(); forms = null; documents.close(); super.onDestroyView() }
}

