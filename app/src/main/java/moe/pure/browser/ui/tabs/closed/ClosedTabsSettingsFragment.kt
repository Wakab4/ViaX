package moe.pure.browser.ui.tabs.closed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import moe.pure.browser.R
import moe.pure.browser.feature.settings.SettingsItem
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.feature.settings.pages.SettingsRecordPage
import moe.pure.browser.feature.settings.ui.SettingsRowAdapter
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.recycler.NativeRecyclerView
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.ui.theme.ThemedActivity

/** hb.e7 is an independent Record page with the original two standard Recycler rows. */
class ClosedTabsSettingsFragment : SettingsRecordPage() {
    private val preferences by lazy { BrowserPreferences(requireContext().getSharedPreferences("browser", android.content.Context.MODE_PRIVATE)) }
    private var list: NativeRecyclerView? = null
    private var adapter: SettingsRowAdapter? = null
    private var dialogs: DialogScope? = null
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        return PageScaffold(activity, getString(R.string.ref_closed_tabs_settings_title), ::closePage, appliesWindowInsets).also { page ->
            val rows = NativeRecyclerView(activity).apply { id = R.id.settings_page_list; layoutManager = LinearLayoutManager(activity); itemAnimator = null; clipToPadding = false }
            adapter = SettingsRowAdapter(activity) {}; rows.adapter = adapter; list = rows; dialogs = DialogScope()
            page.showContent(rows, bottomInsets = rows); NativeToolbarTap.bind(page.toolbar) { rows.smoothScrollToPosition(0) }
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); render() }
    private fun labels() = listOf(R.string.ref_closed_tabs_restore_0, R.string.ref_closed_tabs_restore_1, R.string.ref_closed_tabs_restore_2).map(::getString)
    private fun render() {
        adapter?.submit(listOf(
            SettingsItem.link(2, getString(R.string.ref_closed_tabs_restore_setting), labels()[preferences.restoreTabs]) {
                dialogs?.show(BrowserDialog(requireActivity(), getString(R.string.ref_closed_tabs_restore_setting)).singleChoice(labels(), preferences.restoreTabs) { index ->
                    preferences.restoreTabs = index; render()
                    if (index != 0) BrowserMessages.show(requireActivity(), getString(R.string.ref_closed_tabs_private_restore_notice))
                })
            },
            SettingsItem.toggle(1, getString(R.string.ref_closed_tabs_undo_prompt), preferences.undoClosePrompt, getString(R.string.ref_closed_tabs_undo_prompt_description)) {
                preferences.undoClosePrompt = it; render()
            }
        ))
    }
    override fun onDestroyView() { dialogs?.dispose(); dialogs = null; list?.dispose(); list?.adapter = null; list = null; adapter = null; super.onDestroyView() }
}
