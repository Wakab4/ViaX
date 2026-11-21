package moe.pure.browser.feature.ai.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.feature.ai.model.AiPrompt
import moe.pure.browser.feature.settings.SettingRow
import moe.pure.browser.feature.settings.SettingsItem
import moe.pure.browser.feature.settings.ui.SettingsRowAdapter
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.theme.ThemedActivity

class AiPromptsFragment : AiRecordPage() {
    private var page: AiListPage? = null
    private var adapter: SettingsRowAdapter? = null
    private val ids = HashMap<Int, String>()
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        return AiListPage(activity, getString(R.string.ref_ai_page_prompts), appliesWindowInsets, ::closePage).also {
            page = it; adapter = SettingsRowAdapter(activity) {}; it.list.adapter = adapter
            it.scaffold.addIconAction(R.drawable.ref_ic_add, getString(R.string.ref_search_page_new)) { edit(it.scaffold.toolbar.getChildAt(it.scaffold.toolbar.childCount - 1), null) }
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); load() }
    private fun load() { database({ repository.prompts.all() }) { render(it) } }
    private fun render(prompts: List<AiPrompt>) {
        ids.clear(); val rows = ArrayList<SettingsItem>()
        listOf(AiPrompt.TYPE_SYSTEM, AiPrompt.TYPE_MESSAGE).forEach { type ->
            val group = prompts.filter { it.type == type }
            if (group.isNotEmpty()) rows.add(SettingsItem.section(getString(if (type == AiPrompt.TYPE_SYSTEM) R.string.ref_ai_page_system else R.string.ref_ai_page_message)))
            group.forEach { prompt ->
                val key = prompt.id.hashCode(); ids[key] = prompt.id; val position = rows.size
                rows.add(SettingsItem.Entry(key, SettingRow.Link(prompt.name, onLongClick = {
                    page?.list?.findViewHolderForAdapterPosition(position)?.itemView?.let { actions(it, ids[key]!!) }
                }) { edit(null, ids[key]) }))
            }
        }
        adapter?.submit(rows); page?.empty(prompts.isEmpty())
    }
    private fun actions(anchor: View, id: String) { dialogs.show(BrowserDialog(requireActivity()).items(listOf(getString(R.string.ref_search_page_edit), getString(R.string.ref_search_page_delete))) {
        if (it == 0) edit(null, id) else database({ repository.prompts.delete(id) }) { load() }
    }.anchoredTo(anchor)) }
    private fun edit(anchor: View?, id: String?) {
        parentFragmentManager.setFragmentResultListener(AiPromptEditorFragment.RESULT, this) { _, result ->
            if (result.getString(AiPromptEditorFragment.RESULT) != null) load()
            parentFragmentManager.clearFragmentResultListener(AiPromptEditorFragment.RESULT)
        }
        if (id != null) { RecordPageScope.of(this)?.push(AiPromptEditorFragment::class.java, Bundle().apply { putString("id", id) }); return }
        dialogs.show(BrowserDialog(requireActivity()).items(listOf(getString(R.string.ref_ai_page_system), getString(R.string.ref_ai_page_message))) {
            RecordPageScope.of(this)?.push(AiPromptEditorFragment::class.java, Bundle().apply { putInt("type", if (it == 0) 1 else 2) })
        }.anchoredTo(anchor!!))
    }
    override fun onDestroyView() { page?.close(); page = null; adapter = null; super.onDestroyView() }
}
