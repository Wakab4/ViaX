package moe.via.browser.feature.adblock.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.via.browser.R
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.adblock.AdblockEngine
import moe.via.browser.feature.download.DownloadNames
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import moe.via.browser.feature.scripts.ScriptSettings
import moe.via.browser.feature.search.data.remote.RemoteSearchValues
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.storage.preferences.BrowserPreferences
import moe.via.browser.feature.settings.ui.SettingsRowAdapter
import moe.via.browser.ui.pages.RecordPageScope
import moe.via.browser.ui.theme.ThemedActivity
import java.util.Locale

class AdblockSettingsFragment : AdblockRecordPage() {
    private val values by lazy { requireContext().applicationContext.getSharedPreferences("browser", Context.MODE_PRIVATE) }
    private val preferences by lazy { BrowserPreferences(values) }
    private var page: AdblockListPage? = null
    private var adapter: SettingsRowAdapter? = null
    private var builtinChanged = false
    override fun createPage(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val activity = requireActivity() as ThemedActivity
        return AdblockListPage(activity, getString(R.string.ref_adblock_page_title), appliesWindowInsets, ::closePage).also {
            page = it; adapter = SettingsRowAdapter(activity) {}; it.list.itemAnimator = null; it.list.adapter = adapter
        }.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { super.onViewCreated(view, savedInstanceState); render() }
    private fun render() {
        adapter?.submit(buildList {
            add(SettingsItem.toggle(1, getString(R.string.ref_adblock_page_title), preferences.adblock,
                if (preferences.adblock) getString(R.string.ref_adblock_page_statistics, values.getInt("adblockedtimes", 0), DownloadNames.size(values.getLong("savedata", 0) * 1024))
                else getString(R.string.ref_adblock_page_disabled)) {
                preferences.adblock = it; GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.WEB_SETTINGS); render()
            })
            add(SettingsItem.toggle(2, getString(R.string.ref_adblock_page_builtin), preferences.builtinFilters,
                getString(R.string.ref_adblock_page_builtin_detail)) { builtinChanged = true; preferences.builtinFilters = it; render() })
            add(SettingsItem.link(3, getString(R.string.ref_adblock_page_custom_rules), getString(R.string.ref_adblock_page_custom_rules_detail)) {
                RecordPageScope.of(this@AdblockSettingsFragment)?.push(FilterEditorFragment::class.java, null)
            })
            add(SettingsItem.link(4, getString(R.string.ref_adblock_page_subscriptions), getString(R.string.ref_adblock_page_subscriptions_detail)) {
                RecordPageScope.of(this@AdblockSettingsFragment)?.push(FilterSubscriptionsFragment::class.java, null)
            })
            if (Locale.getDefault().country.equals("CN", true) && RemoteSearchValues.fulltextAllowed) {
                add(SettingsItem.toggle(5, getString(R.string.ref_adblock_page_fulltext), ScriptSettings.fulltext(requireContext()),
                    getString(R.string.ref_adblock_page_fulltext_detail), preferences.scriptsEnabled) {
                    ScriptSettings.fulltext(requireContext(), it); render()
                })
            }
        })
    }
    override fun onPause() {
        if (builtinChanged) {
            val app = requireContext().applicationContext
            BrowserExecutors.io.execute { AdblockEngine.get(app).useBuiltin(preferences.builtinFilters) }
        }
        super.onPause()
    }
    override fun onDestroyView() { page?.close(); page = null; adapter = null; super.onDestroyView() }
}
