package moe.pure.browser.feature.search.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.feature.search.data.SearchBuiltins
import moe.pure.browser.feature.search.model.SearchEngine
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.theme.ThemedActivity

class SearchEnginesFragment : SearchRecordPage() {
    private var page: SearchListPage? = null
    private var adapter: SearchEngineListAdapter? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        val rows = SearchEngineListAdapter(activity, ::select, ::actions).also { adapter = it }
        return SearchListPage(activity, getString(R.string.ref_search_page_engines), appliesWindowInsets, ::closePage).also {
            page = it; it.list.adapter = rows
            it.scaffold.addIconAction(R.drawable.ref_ic_add, getString(R.string.ref_search_page_new)) { edit(0) }
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter!!.apply { selectedId = preferences.engineId; shortcuts = preferences.shortcuts(); entries.addAll(repository.all()); notifyDataSetChanged() }
    }
    private fun select(engine: SearchEngine) {
        val rows = adapter ?: return
        if (rows.selectedId == engine.id) return
        storeSelection(engine); rows.select(engine.id)
    }
    private fun storeSelection(engine: SearchEngine) {
        if (engine.id > 0 || engine.id <= -999) preferences.customUrl = engine.mobileTemplate
        preferences.engineId = engine.id
    }
    private fun actions(anchor: View, position: Int) {
        val engine = adapter!!.entries[position]
        if (engine.id <= 0) { edit(engine.id); return }
        dialogs.show(BrowserDialog(requireActivity()).items(listOf(getString(R.string.ref_search_page_edit), getString(R.string.ref_search_page_delete))) {
            if (it == 0) edit(engine.id) else remove(engine.id, position)
        }.anchoredTo(anchor))
    }
    private fun remove(id: Int, position: Int) {
        val rows = adapter!!
        if (id == rows.selectedId) {
            val default = SearchBuiltins.defaultId()
            val target = rows.entries.withIndex().firstOrNull { it.index != position && it.value.id == default }?.index ?: 0
            select(rows.entries[target])
        }
        repository.delete(id)
        rows.shortcuts = rows.shortcuts.toMutableMap().apply { remove(id) }.also(preferences::writeShortcuts)
        rows.entries.removeAt(position); rows.notifyItemRemoved(position)
    }
    private fun edit(id: Int) {
        parentFragmentManager.setFragmentResultListener(SearchEngineEditorFragment.RESULT, this) { _, result ->
            val resultId = result.getInt(SearchEngineEditorFragment.RESULT, 0)
            val rows = adapter!!; rows.shortcuts = preferences.shortcuts()
            if (resultId > 0) {
                val at = rows.position(resultId)
                database({ repository.get(resultId) }) { engine -> if (engine != null) {
                    if (at < 0) { val added = rows.entries.size; rows.entries.add(engine); rows.notifyItemInserted(added); page?.list?.scrollToPosition(added) }
                    else { rows.entries[at] = engine; rows.notifyItemChanged(at); if (rows.selectedId == resultId) storeSelection(engine) }
                } }
            } else { val at = rows.position(resultId); if (at >= 0) rows.notifyItemChanged(at) }
            parentFragmentManager.clearFragmentResultListener(SearchEngineEditorFragment.RESULT)
        }
        RecordPageScope.of(this)?.push(SearchEngineEditorFragment::class.java, Bundle().apply { putInt("id", id) })
    }
    override fun onDestroyView() { page?.close(); page = null; adapter = null; super.onDestroyView() }
}
