package moe.via.browser.feature.library.pages

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.feature.library.ui.LibraryAdapter
import moe.via.browser.feature.library.LibraryEntry as Entry
import moe.via.browser.feature.library.ui.LibraryRow
import moe.via.browser.feature.library.LibraryTab
import moe.via.browser.feature.library.history.HistoryOwner
import moe.via.browser.feature.library.history.HistoryPager
import moe.via.browser.feature.library.history.ui.HistoryClearController
import moe.via.browser.feature.library.history.ui.HistoryLoadMoreController
import moe.via.browser.feature.library.history.ui.HistoryTailAdapter
import moe.via.browser.feature.library.navigation.HistoryContextMenuController
import moe.via.browser.feature.library.navigation.LibraryOpenWhere
import moe.via.browser.feature.library.ui.recycler.LibraryDateDecoration
import moe.via.browser.feature.library.ui.recycler.LibraryProcessState
import moe.via.browser.ui.components.recycler.NativeRangeSelection
import moe.via.browser.ui.message.BrowserMessages

class HistoryPageFragment : LibraryChildFragment() {
    override val tab = LibraryTab.HISTORY
    private lateinit var owner: HistoryOwner
    private lateinit var adapter: LibraryAdapter
    private lateinit var dates: LibraryDateDecoration
    private lateinit var range: NativeRangeSelection
    private lateinit var more: HistoryLoadMoreController
    private lateinit var clear: HistoryClearController
    private lateinit var contextMenu: HistoryContextMenuController
    private var editing = false
    private var rowsSeen = false
    private var empty = false
    private val main = Handler(Looper.getMainLooper())
    private val searchWork = Runnable { if (rowsSeen) { owner.pager.search(query); exitEditing() } }
    private val observer = object : HistoryPager.Observer {
        override fun rows(values: List<Entry>) {
            val first = !rowsSeen; rowsSeen = true; empty = values.isEmpty(); page?.historyRows(empty, query.isEmpty())
            dates.submit(values); adapter.rows = values.map { LibraryRow.Item(it) }; page?.list?.invalidateItemDecorations()
            if (first) LibraryProcessState.read(tab)?.scroll?.restore(page!!.list)
            update()
        }
        override fun removed(values: List<Entry>, positions: List<Int>) {
            dates.submit(values); adapter.replaceBacking(values.map { LibraryRow.Item(it) }); adapter.removedPositions(positions); page?.list?.invalidateItemDecorations(); update()
        }
        override fun backing(values: List<Entry>) { dates.submit(values); adapter.replaceBacking(values.map { LibraryRow.Item(it) }) }
        override fun hasMore(value: Boolean) { more.hasMore(value) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        owner = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = HistoryOwner(requireContext()) as T
        })[HistoryOwner::class.java]
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState); editing = false; rowsSeen = false
        adapter = LibraryAdapter(requireActivity(), object : LibraryAdapter.Actions {
            override fun click(position: Int) {
                if (editing) { adapter.select(position, !adapter.isSelected(position)); update() }
                else model.open(owner.pager.records?.getOrNull(position)?.url, LibraryOpenWhere.CURRENT)
            }
            override fun longClick(view: View, position: Int): Boolean { if (editing) range.start(position) else contextMenu.show(view, position); return true }
            override fun drag(holder: RecyclerView.ViewHolder) = Unit
        }).apply { tab = LibraryTab.HISTORY; draggingAllowed = false }
        dates = LibraryDateDecoration(requireContext()); page!!.list.addItemDecoration(dates); page!!.list.setHasFixedSize(true)
        val tail = HistoryTailAdapter(adapter); page!!.list.adapter = tail
        more = HistoryLoadMoreController(page!!.list, { tail.state = it }, { owner.pager.load() })
        range = NativeRangeSelection(page!!.list, object : NativeRangeSelection.Actions {
            override fun selectable(position: Int) = editing && position in adapter.rows.indices
            override fun selected(position: Int) = adapter.isSelected(position)
            override fun select(position: Int, value: Boolean) { adapter.select(position, value); update() }
        }, stateChanged = { parent.paging(this, !editing && !it) })
        clear = HistoryClearController(requireActivity(), owner.pager, object : HistoryClearController.Host {
            override fun anchor() = page!!.historyActionAnchor
            override fun searching() = query.isNotEmpty()
            override fun exitEditing() = this@HistoryPageFragment.exitEditing()
        })
        contextMenu = HistoryContextMenuController(requireActivity(), object : HistoryContextMenuController.Host {
            override fun url(position: Int) = owner.pager.records?.getOrNull(position)?.url
            override fun open(position: Int, where: LibraryOpenWhere) = model.open(url(position), where)
            override fun delete(position: Int) = owner.pager.removeOne(position)
        }) { BrowserMessages.show(requireActivity(), it) }
        owner.pager.attach(observer)
        if (!owner.opened) { owner.opened = true; owner.pager.open(LibraryProcessState.read(tab)?.scroll?.position, query) }
        more.active = true; update()
    }
    private fun update() {
        adapter.editing = editing; page?.update(editing, owner.pager.records?.isNotEmpty() == true, adapter.selectionCount, empty, owner.pager.records?.size ?: 0)
        parent.paging(this, !editing)
    }
    private fun exitEditing() { range.finish(); editing = false; adapter.clearSelection(); update() }
    override fun search(value: String) { main.removeCallbacks(searchWork); main.postDelayed(searchWork, 100) }
    override fun footerLeft() {
        if (editing) { val select = adapter.selectionCount != (owner.pager.records?.size ?: 0); adapter.rows.indices.forEach { adapter.select(it, select) }; update() }
        else model.events.value = LibraryModel.Event.RESTORE_TABS
    }
    override fun footerCenter() { if (editing) clear.selected(adapter.historySelected.toList()) else clear.clear() }
    override fun footerRight() { range.finish(); editing = !editing; if (!editing) adapter.clearSelection(); update() }
    override fun footerMove() = Unit
    override fun footerOpen() = Unit
    internal override fun pagingAllowed() = !editing
    internal override fun canSwipeBack() = !editing && super.canSwipeBack()
    internal override fun back(): Boolean { if (editing) { exitEditing(); return true }; return super.back() }
    override fun onDestroyView() {
        main.removeCallbacksAndMessages(null); owner.pager.detach(observer); more.close(); clear.close(); contextMenu.close(); range.close(); adapter.close(); super.onDestroyView()
    }
}
