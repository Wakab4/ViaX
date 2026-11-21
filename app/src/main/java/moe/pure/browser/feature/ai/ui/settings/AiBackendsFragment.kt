package moe.pure.browser.feature.ai.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.feature.ai.model.AiBackend
import moe.pure.browser.feature.ai.prompt.AiTemplates
import moe.pure.browser.feature.settings.SettingRow
import moe.pure.browser.feature.settings.SettingsItem
import moe.pure.browser.feature.settings.ui.SettingsRowAdapter
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.theme.ThemedActivity

class AiBackendsFragment : AiRecordPage() {
    private var page: AiListPage? = null
    private var adapter: SettingsRowAdapter? = null
    private val ids = HashMap<Int, String>()
    private var selected: String? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        return AiListPage(activity, getString(R.string.ref_ai_page_backends), appliesWindowInsets, ::closePage).also {
            page = it; adapter = SettingsRowAdapter(activity) {}; it.list.adapter = adapter
            it.scaffold.addIconAction(R.drawable.ref_ic_add, getString(R.string.ref_search_page_new)) { edit(null) }
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); load() }
    private fun load() { database({ repository.backends.all() }) { selected = preferences.backendId; render(it) } }
    private fun render(backends: List<AiBackend>) {
        ids.clear()
        val valid = backends.any { it.id == selected }
        if (!valid && selected != null) selected = null
        val rows = ArrayList<SettingsItem>()
        rows.add(SettingsItem.radio(0, getString(R.string.ref_ai_page_none), !valid) { select(ids[0]) })
        backends.forEachIndexed { index, backend ->
            val key = backend.id.hashCode(); ids[key] = backend.id
            rows.add(SettingsItem.Entry(key, SettingRow.Radio(backend.name, backend.id == selected, onLongClick = {
                page?.list?.findViewHolderForAdapterPosition(index + 1)?.itemView?.let { actions(it, ids[key]!!) }
            }) { select(ids[key]) }))
        }
        adapter?.submit(rows)
    }
    private fun select(id: String?) {
        if (if (id == null) selected.isNullOrEmpty() else id == selected) return
        preferences.backendId = id; load()
    }
    private fun actions(anchor: View, id: String) {
        dialogs.show(BrowserDialog(requireActivity()).items(listOf(getString(R.string.ref_search_page_edit), getString(R.string.ref_search_page_delete))) {
            if (it == 0) edit(id) else database({ repository.backends.delete(id) }) { if (id == selected) select(null) else load() }
        }.anchoredTo(anchor))
    }
    private fun edit(id: String?) {
        parentFragmentManager.setFragmentResultListener(AiBackendEditorFragment.RESULT, this) { _, result ->
            if (result.getString("id") != null) load()
            parentFragmentManager.clearFragmentResultListener(AiBackendEditorFragment.RESULT)
        }
        fun open(value: String?) { RecordPageScope.of(this)?.push(AiBackendEditorFragment::class.java, value?.takeIf(String::isNotEmpty)?.let { Bundle().apply { putString("id", it) } }) }
        if (id != null) { open(id); return }
        val templates = AiTemplates.providers()
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_search_page_new)).items(listOf(getString(R.string.ref_ai_page_custom)) + templates.map { it.name }) {
            open(if (it == 0) null else templates[it - 1].id)
        })
    }
    override fun onDestroyView() { page?.close(); page = null; adapter = null; super.onDestroyView() }
}
