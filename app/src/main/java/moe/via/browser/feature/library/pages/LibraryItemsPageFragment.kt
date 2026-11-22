package moe.via.browser.feature.library.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.feature.home.data.NativeHomeFavorites
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import moe.via.browser.feature.library.ui.LibraryAdapter
import moe.via.browser.feature.library.LibraryEntry as Entry
import moe.via.browser.feature.library.LibraryKind
import moe.via.browser.feature.library.ui.LibraryRow
import moe.via.browser.feature.library.LibraryTab
import moe.via.browser.feature.library.navigation.LibraryContextMenuController
import moe.via.browser.feature.library.navigation.LibraryOpenRequest
import moe.via.browser.feature.library.navigation.LibraryOpenWhere
import moe.via.browser.ui.components.recycler.NativeRangeSelection
import moe.via.browser.ui.message.BrowserMessages

/** Common item presentation; bookmark traversal and offline queries remain separate child owners. */
abstract class LibraryItemsPageFragment : LibraryChildFragment() {
    internal lateinit var adapter: LibraryAdapter
    internal lateinit var range: NativeRangeSelection
    protected var entries: List<Entry> = emptyList()
    protected var editing = false
    private var rowContext: LibraryContextMenuController? = null
    private val main = Handler(Looper.getMainLooper())
    private val queryWork = Runnable { reload() }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = LibraryAdapter(requireActivity(), object : LibraryAdapter.Actions {
            override fun click(position: Int) = itemClick(position)
            override fun longClick(view: View, position: Int) = itemLongClick(view, position)
            override fun drag(holder: RecyclerView.ViewHolder) = this@LibraryItemsPageFragment.drag(holder)
        }).apply { this.tab = this@LibraryItemsPageFragment.tab }
        page!!.list.adapter = adapter
        range = NativeRangeSelection(page!!.list, object : NativeRangeSelection.Actions {
            override fun selectable(position: Int) = editing && (adapter.rows.getOrNull(position) as? LibraryRow.Item)?.immutable == false
            override fun selected(position: Int) = adapter.isSelected(position)
            override fun select(position: Int, value: Boolean) { adapter.select(position, value); update() }
        }, stateChanged = { parent.paging(this, !editing && !it) })
        rowContext = LibraryContextMenuController(requireActivity(), object : LibraryContextMenuController.Host {
            override fun open(entry: Entry, where: LibraryOpenWhere) {
                if (!entry.isFolder) model.open(entry.url, where)
                else work({ repository.bookmarkUrls(entry.id) }) { urls -> model.open(urls.mapIndexed { index, url -> LibraryOpenRequest(url, if (index == 0) where else LibraryOpenWhere.FORCE_CURRENT) }) }
            }
            override fun edit(entry: Entry) = editEntry(entry)
            override fun favorite(entry: Entry) {
                val url = if (!entry.isFolder) entry.url else if (entry.id.isEmpty()) "v://bookmarks" else "v://bookmarks/?folder=${entry.id}&folderName=${Uri.encode(entry.title)}"
                val app = requireContext().applicationContext
                work({ NativeHomeFavorites.get(app).save(entry.title, url) }) { saved -> if (saved > 0) {
                    GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.HOME); BrowserMessages.show(requireActivity(), R.string.ref_library_dialog_added_home)
                } }
            }
            override fun delete(entries: List<Entry>) { work({ repository.delete(entries) }) { reload() } }
            override fun copy(entry: Entry) {
                requireActivity().getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(null, entry.url))
                BrowserMessages.show(requireActivity(), R.string.ref_library_dialog_copied)
            }
            override fun share(entry: Entry) { startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, entry.url) }, null)) }
        })
    }
    protected abstract fun reload()
    protected open fun editEntry(entry: Entry) = Unit
    protected open fun openFolder(entry: Entry) = Unit
    protected open fun drag(holder: RecyclerView.ViewHolder) = Unit
    protected fun show(items: List<Entry>, parentEntry: Entry? = null) {
        entries = items
        if (items.isEmpty()) editing = false
        if (editing) adapter.selected.clear() else adapter.selected.retainAll(items.map { it.key }.toSet())
        adapter.rows = buildList { if (parentEntry != null) add(LibraryRow.Item(parentEntry, immutable = true)); items.forEach { add(LibraryRow.Item(it)) } }
        update()
    }
    private fun itemClick(position: Int) {
        val item = adapter.rows.getOrNull(position) as? LibraryRow.Item ?: return
        if (editing) { if (!item.immutable) { adapter.select(position, !adapter.isSelected(position)); update() }; return }
        if (item.entry.isFolder) openFolder(item.entry)
        else model.open(if (item.entry.kind == LibraryKind.OFFLINE) Uri.fromFile(java.io.File(item.entry.path)).toString() else item.entry.url, LibraryOpenWhere.CURRENT)
    }
    private fun itemLongClick(view: View, position: Int): Boolean {
        val row = adapter.rows.getOrNull(position) as? LibraryRow.Item ?: return false
        if (editing) { range.start(position); return true }
        if (row.immutable) return false
        rowContext?.show(view, row.entry); return true
    }
    protected open fun update() {
        adapter.editing = editing; page?.update(editing, entries.isNotEmpty(), adapter.selectionCount)
        parent.paging(this, !editing)
    }
    override fun search(value: String) { range.finish(); main.removeCallbacks(queryWork); main.postDelayed(queryWork, 100) }
    override fun footerLeft() { if (editing) { val select = adapter.selectionCount != entries.size; adapter.rows.indices.forEach { adapter.select(it, select) }; update() } }
    override fun footerCenter() { val selecting = editing; rowContext?.deleteSelected { if (selecting) adapter.selectedEntries() else entries } }
    override fun footerRight() { range.finish(); editing = !editing; if (!editing) adapter.clearSelection(); update() }
    override fun footerMove() = Unit
    override fun footerOpen() {
        val selected = adapter.selectedEntries()
        work({ selected.flatMap { if (it.isFolder) repository.bookmarks(it.id).filterNot(Entry::isFolder) else listOf(it) } }) { pages ->
            model.open(pages.filterNot(Entry::isFolder).map { LibraryOpenRequest(if (it.kind == LibraryKind.OFFLINE) Uri.fromFile(java.io.File(it.path)).toString() else it.url, LibraryOpenWhere.FOREGROUND) })
        }
    }
    internal override fun back(): Boolean {
        if (editing) { range.finish(); editing = false; adapter.clearSelection(); update(); return true }
        return super.back()
    }
    internal override fun canSwipeBack() = !editing && super.canSwipeBack()
    internal override fun pagingAllowed() = !editing
    override fun onDestroyView() {
        main.removeCallbacksAndMessages(null); rowContext?.close(); rowContext = null; range.close(); adapter.close(); super.onDestroyView()
    }
}
