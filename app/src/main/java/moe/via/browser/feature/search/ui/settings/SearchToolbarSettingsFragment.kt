package moe.via.browser.feature.search.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.search.model.SearchEngine
import moe.via.browser.ui.components.recycler.NativeRangeSelection
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.palette

/** hb.i6 owns a real RecyclerView drag helper and n8 range selection over the same adapter. */
class SearchToolbarSettingsFragment : SearchRecordPage() {
    private var page: SearchListPage? = null
    private var adapter: SearchToolbarAdapter? = null
    private var helper: ItemTouchHelper? = null
    private var selection: NativeRangeSelection? = null
    private val engines = ArrayList<SearchEngine>()
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val rows = SearchToolbarAdapter(activity, {
            preferences.toolbarEnabled = !preferences.toolbarEnabled; render()
        }, { position ->
            val item = adapter!!.entries[position - 2]
            if (!item.default) { adapter!!.select(position, !item.checked); saveHidden() }
        }, { position -> selection?.start(position) }, { holder -> helper?.startDrag(holder) }).also { adapter = it }
        return SearchListPage(activity, getString(R.string.ref_search_page_toolbar), appliesWindowInsets, ::closePage).also { page = it; it.list.adapter = rows }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val list = page!!.list
        helper = ItemTouchHelper(Drag()).also { it.attachToRecyclerView(list) }
        selection = NativeRangeSelection(list, object : NativeRangeSelection.Actions {
            override fun selectable(position: Int) = position >= 2
            override fun selected(position: Int) = position >= 2 && adapter!!.entries[position - 2].checked
            override fun select(position: Int, value: Boolean) { if (position >= 2) adapter!!.select(position, value) }
        }) { active -> dragLayout?.gestures(!active); if (!active) saveHidden() }
        database({ repository.all() }) { items -> engines.clear(); engines.addAll(items); render() }
    }
    private fun render() {
        val hidden = preferences.toolbarDisabledIds
        adapter?.submit(preferences.ordered(engines).map { engine ->
            val default = engine.id == preferences.engineId
            SearchToolbarAdapter.Item(engine, default, default || engine.id !in hidden)
        }, preferences.toolbarEnabled)
    }
    private fun saveHidden() { adapter?.let { rows -> preferences.toolbarDisabledIds = rows.entries.filter { !it.checked }.map { it.engine.id } } }
    private fun saveOrder() {
        val order = adapter!!.entries.map { it.engine.id }; preferences.toolbarOrder = order
        NativeLog.debug(null, "update order: ${order.joinToString(",")}")
    }
    private inner class Drag : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        private var moved = false
        override fun isLongPressDragEnabled() = false
        override fun onMove(list: RecyclerView, current: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val from = current.bindingAdapterPosition; val to = target.bindingAdapterPosition
            if (from == to || from < 2 || to < 2) return false
            adapter!!.notifyItemMoved(from, to); return true
        }
        override fun onMoved(list: RecyclerView, current: RecyclerView.ViewHolder, from: Int, target: RecyclerView.ViewHolder, to: Int, x: Int, y: Int) {
            super.onMoved(list, current, from, target, to, x, y)
            adapter!!.moved(from, to); moved = true
        }
        override fun onSelectedChanged(holder: RecyclerView.ViewHolder?, state: Int) {
            super.onSelectedChanged(holder, state)
            if (state == ItemTouchHelper.ACTION_STATE_DRAG) holder?.itemView?.setBackgroundColor(requireContext().palette.canvas)
            if (moved && state == ItemTouchHelper.ACTION_STATE_IDLE) { moved = false; saveOrder() }
        }
        override fun clearView(list: RecyclerView, holder: RecyclerView.ViewHolder) { super.clearView(list, holder); holder.itemView.setBackgroundResource(R.drawable.ref_redirect_page_feedback) }
        override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = Unit
    }
    override fun onDestroyView() {
        selection?.close(); selection = null; helper?.attachToRecyclerView(null); helper = null
        page?.close(); page = null; adapter = null; super.onDestroyView()
    }
}
