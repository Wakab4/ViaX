package moe.pure.browser.feature.ai.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.feature.settings.SettingsItem
import moe.pure.browser.feature.settings.ui.SettingsRowAdapter
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.theme.ThemedActivity

class AiSettingsFragment : AiRecordPage() {
    private var page: AiListPage? = null
    private var adapter: SettingsRowAdapter? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        return AiListPage(activity, getString(R.string.ref_ai_page_settings), appliesWindowInsets, ::closePage).also {
            page = it; adapter = SettingsRowAdapter(activity) {}; it.list.adapter = adapter
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); refresh() }
    override fun onHiddenChanged(hidden: Boolean) { super.onHiddenChanged(hidden); if (!hidden && view != null) refresh() }
    private fun refresh() {
        render(null)
        val id = preferences.backendId
        if (!id.isNullOrEmpty()) database({ repository.backends.get(id) }) { render(it?.name) }
    }
    private fun render(name: String?) { adapter?.submit(listOf(
        SettingsItem.link(1, getString(R.string.ref_ai_page_backends), name ?: getString(R.string.ref_ai_page_none)) { RecordPageScope.of(this)?.push(AiBackendsFragment::class.java, null) },
        SettingsItem.link(2, getString(R.string.ref_ai_page_prompts)) { RecordPageScope.of(this)?.push(AiPromptsFragment::class.java, null) }
    )) }
    override fun onDestroyView() { page?.close(); page = null; adapter = null; super.onDestroyView() }
}
