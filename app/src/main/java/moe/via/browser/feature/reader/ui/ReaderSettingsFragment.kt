package moe.via.browser.feature.reader.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import moe.via.browser.R
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import moe.via.browser.feature.reader.ReaderPreferences
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.pages.SettingsRecordPage
import moe.via.browser.feature.settings.ui.SettingsRowAdapter
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.via.browser.ui.components.recycler.NativeRecyclerView
import moe.via.browser.ui.theme.ThemedActivity

/** hb.o5's five recycled rows. Color and size rows keep their live widget state when the switch changes. */
class ReaderSettingsFragment : SettingsRecordPage() {
    private val preferences by lazy { ReaderPreferences(requireContext()) }
    private var list: NativeRecyclerView? = null
    private var adapter: SettingsRowAdapter? = null
    private var initialColor = 0
    private var initialSize = 17
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        initialColor = preferences.themeColor; initialSize = preferences.textSize
        val page = PageScaffold(activity, getString(R.string.ref_reader_settings_title), ::closePage, appliesWindowInsets)
        val rows = SettingsRowAdapter(activity, ::render).also { adapter = it }
        val recycler = NativeRecyclerView(activity).apply {
            id = R.id.settings_page_list; layoutManager = LinearLayoutManager(activity); clipToPadding = false
            NativeRecyclerEdgeEffect.install(this); adapter = rows
        }.also { list = it }
        page.showContent(recycler, bottomInsets = recycler)
        NativeToolbarTap.bind(page.toolbar) { recycler.smoothScrollToPosition(0) }
        return page.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); render() }
    private fun render() {
        val confirmation = preferences.confirmEntry
        val activity = requireActivity() as ThemedActivity
        adapter?.submit(listOf(
            SettingsItem.toggle(1, getString(R.string.ref_reader_settings_confirmation), confirmation,
                getString(if (confirmation) R.string.ref_reader_settings_confirmation_on else R.string.ref_reader_settings_confirmation_off)) { preferences.confirmEntry = it },
            SettingsItem.section(getString(R.string.ref_reader_settings_appearance)),
            SettingsItem.content("reader_colors", initialColor) {
                ReaderColorPicker(activity, initialColor) { preferences.themeColor = it; dirty() }
            },
            SettingsItem.content("reader_size", initialSize) {
                ReaderFontSizeSettingView(activity, initialSize) { preferences.textSize = it; dirty() }
            },
            SettingsItem.link(3, getString(R.string.ref_reader_settings_css)) { ReaderCssNavigation.open(this, ::dirty) }
        ))
    }
    private fun dirty() = GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.PAGE_EFFECTS)
    override fun onDestroyView() { list?.dispose(); list?.adapter = null; list = null; adapter = null; super.onDestroyView() }
}
