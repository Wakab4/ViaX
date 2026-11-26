package moe.via.browser.feature.settings.ui

import android.os.Build
import moe.via.browser.R
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.SettingsRoute
import moe.via.browser.feature.home.data.HomePreferences

internal class CoreSettingsPages(private val host: SettingsHost) {
    private val prefs get() = host.preferences
    fun root(): List<SettingsItem> = with(host) {
        buildList {
            add(SettingsItem.section(host.text(R.string.settings_group_browser)))
            add(link(1, host.text(R.string.settings_general), SettingsRoute.GENERAL))
            add(SettingsItem.link(8, host.text(R.string.ref_home_design_title)) { command(SettingsCommand.CUSTOMIZE) })
            add(link(3, host.text(R.string.settings_advanced), SettingsRoute.ADVANCED))
            add(SettingsItem.link(9, host.text(R.string.ref_menu_scripts)) { command(SettingsCommand.SCRIPTS) })

            add(SettingsItem.section(host.text(R.string.settings_group_network)))
            add(link(14, host.text(R.string.proxy_title), SettingsRoute.PROXY))
            add(link(15, host.text(R.string.web_dns_title), SettingsRoute.WEB_DNS))

            add(SettingsItem.section(host.text(R.string.settings_group_more)))
            add(link(2, host.text(R.string.settings_privacy), SettingsRoute.PRIVACY))
            add(link(13, host.text(R.string.settings_site_data), SettingsRoute.SITE_DATA))
            if (prefs.labsEnabled) add(link(12, host.text(R.string.settings_labs), SettingsRoute.LABS))
            add(SettingsItem.link(5, host.text(R.string.ref_about_page_title)) { command(SettingsCommand.ABOUT) })
        }
    }
    fun privacy(): List<SettingsItem> = with(host) { listOf(
        toggle(1, host.text(R.string.settings_dnt), prefs.doNotTrack) { prefs.doNotTrack = it },
        toggle(6, host.text(R.string.settings_disable_webrtc), prefs.disableWebRtc) { prefs.disableWebRtc = it },
        toggle(4, host.text(R.string.settings_gpc), prefs.globalPrivacyControl, host.text(R.string.settings_gpc_detail)) { prefs.globalPrivacyControl = it }
    ) }
    fun advanced(): List<SettingsItem> = with(host) { buildList {
        add(toggle(4, host.text(R.string.settings_save_data), prefs.saveData, host.text(R.string.settings_save_data_detail)) { prefs.saveData = it })
        add(toggle(1, host.text(R.string.settings_auto_sniff), prefs.autoSniff) { prefs.autoSniff = it })
        add(toggle(3, host.text(R.string.settings_web_debug), prefs.webDebugging) {
            prefs.webDebugging = it
            if (it) message(host.text(R.string.settings_web_debug), host.text(R.string.settings_web_debug_help))
        })
        if (Build.VERSION.SDK_INT >= 33) add(toggle(8, host.text(R.string.settings_predictive_back), prefs.predictiveBackDisabled) { prefs.predictiveBackDisabled = it })
        add(toggle(7, host.text(R.string.settings_disable_custom_tabs), prefs.customTabsDisabled, host.text(R.string.settings_disable_custom_tabs_detail)) { prefs.customTabsDisabled = it })
        add(toggle(5, host.text(R.string.settings_disable_safe_browsing), prefs.safeBrowsingDisabled, host.text(R.string.settings_safe_browsing_detail)) { prefs.safeBrowsingDisabled = it })
        add(SettingsItem.link(6, host.text(R.string.settings_ssl), host.text(R.string.settings_ssl_summary)) { command(SettingsCommand.SSL) })
    } }
    fun labs(): List<SettingsItem> = with(host) { buildList {
        add(link(2, host.text(R.string.ref_skin_title), SettingsRoute.SKINS))
        if (Build.VERSION.SDK_INT >= 31) add(toggle(1, host.text(R.string.settings_blur), prefs.blurEnabled, host.text(R.string.settings_blur_detail)) { prefs.blurEnabled = it })
        if (Build.VERSION.SDK_INT >= 29) add(toggle(3, host.text(R.string.settings_settings_background), prefs.settingsBackground) {
            prefs.settingsBackground = it
            message(host.text(R.string.settings_settings_background), host.text(R.string.settings_settings_background_hint))
        })
    } }
    fun homepage() {
        val labels = listOf(host.text(R.string.ref_font_default), host.text(R.string.ref_blank_page_title), host.text(R.string.ref_menu_bookmarks), host.text(R.string.settings_sample_page))
        val urls = listOf("about:home", "about:blank", "about:bookmarks")
        val selected = urls.indexOf(prefs.homepage).takeIf { it >= 0 } ?: 3
        host.choose(host.text(R.string.ref_home_page_title), labels, selected) { index ->
            if (index < 3) setHomepage(urls[index]) else customHomepage()
        }
    }
    private fun setHomepage(url: String) { HomePreferences(host.activity).homepage = url; host.refresh() }
    private fun customHomepage() {
        val dialog = moe.via.browser.ui.dialog.BrowserDialog(host.activity, host.text(R.string.settings_homepage_custom))
        val field = dialog.field(prefs.homepage.takeUnless { it.startsWith("about:") }.orEmpty(), host.text(R.string.settings_homepage_custom), 3)
        host.activity.dialogScope.show(dialog.positive(host.text(android.R.string.ok)) {
            val text = field.text.toString().trim()
            val resolved = moe.via.browser.feature.search.resolve.SearchUrlResolver.resolve(text, "https://www.google.com/search?q=%s")?.url.orEmpty()
            if (resolved.contains("://")) { setHomepage(resolved); true } else false
        }.negative())
    }
}
