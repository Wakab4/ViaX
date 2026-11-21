package moe.pure.browser.feature.search.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.pure.browser.R
import moe.pure.browser.feature.internalpages.GeneratedPageDirtyState
import moe.pure.browser.feature.settings.SettingsItem
import moe.pure.browser.feature.settings.ui.SettingsRowAdapter
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.theme.ThemedActivity

class SearchSettingsFragment : SearchRecordPage() {
    private var page: SearchListPage? = null
    private var adapter: SettingsRowAdapter? = null
    private var engineName: String? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        return SearchListPage(activity, getString(R.string.ref_search_page_title), appliesWindowInsets, ::closePage).also {
            page = it; adapter = SettingsRowAdapter(activity) {}; it.list.adapter = adapter
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); refresh() }
    override fun onHiddenChanged(hidden: Boolean) { super.onHiddenChanged(hidden); if (!hidden && view != null) refresh() }
    private fun refresh() { database({ repository.selected().name }) { engineName = it; render() } }
    private fun sources() = intArrayOf(R.string.ref_search_page_favorites, R.string.ref_search_page_bookmarks,
        R.string.ref_search_page_tabs, R.string.ref_search_page_history, R.string.ref_search_page_engines, R.string.ref_search_page_search_history).map(::getString)
    private fun render() {
        val mask = preferences.suggestionMask
        adapter?.submit(listOf(
            SettingsItem.link(1, getString(R.string.ref_search_page_engines), engineName.orEmpty()) {
                RecordPageScope.of(this)?.push(SearchEnginesFragment::class.java, null)
            },
            SettingsItem.link(2, getString(R.string.ref_search_page_suggestions), sources().filterIndexed { index, _ -> mask and (1 shl index) != 0 }.joinToString("/"), onClick = ::chooseSuggestions),
            SettingsItem.link(3, getString(R.string.ref_search_page_toolbar), getString(if (preferences.toolbarEnabled) R.string.ref_search_page_enabled else R.string.ref_search_page_disabled)) {
                RecordPageScope.of(this)?.push(SearchToolbarSettingsFragment::class.java, null)
            }
        ))
    }
    private fun chooseSuggestions() {
        val selected = (0..5).filterTo(mutableSetOf()) { preferences.suggestionMask and (1 shl it) != 0 }
        dialogs.show(BrowserDialog(requireActivity(), getString(R.string.ref_search_page_suggestions), getString(R.string.ref_search_page_suggestion_hint))
            .multipleChoice(sources(), selected).positive(getString(android.R.string.ok)) { dialog ->
                preferences.suggestionMask = dialog.selectedIndices.sumOf { 1 shl it }
                GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.HOME); render(); true
            }.negative())
    }
    override fun onDestroyView() { page?.close(); page = null; adapter = null; super.onDestroyView() }
}
