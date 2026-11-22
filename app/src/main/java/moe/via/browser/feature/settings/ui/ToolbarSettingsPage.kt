package moe.via.browser.feature.settings.ui

import moe.via.browser.R
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.ToolbarPreviewState
import moe.via.browser.ui.theme.window.ChromeLayoutPolicy
import moe.via.browser.ui.dialog.BrowserDialog

internal class ToolbarSettingsPage(private val host: SettingsHost) {
    private val prefs get() = host.preferences
    private val layout get() = prefs.toolbarLayout.takeIf { it in 0..3 } ?: if (ChromeLayoutPolicy.resolve(host.activity).tablet) 1 else 0
    fun rows(): List<SettingsItem> = with(host) {
        val preview = ToolbarPreviewState(layout, prefs.tabBar && layout in 1..2, prefs.addressContent)
        listOf(
        SettingsItem.content(ToolbarPreviewState::class, preview) { ToolbarPreview(activity, preview) },
        choice(1, host.text(R.string.settings_layout), listOf(host.text(R.string.settings_layout_traditional), host.text(R.string.settings_layout_top), host.text(R.string.settings_layout_bottom), host.text(R.string.settings_layout_double)), layout) {
            prefs.toolbarLayout = it
            if (it !in 1..2) prefs.tabBar = false
        },
        SettingsItem.link(2, host.text(R.string.settings_auto_hide), listOf(host.text(R.string.settings_show_always), host.text(R.string.settings_show_on_swipe), host.text(R.string.settings_show_on_tap))[prefs.autoHide.coerceIn(0, 2)]) {
            val preview = AutoHideChoices(activity, prefs.autoHide) { selected -> prefs.autoHide = selected; refresh() }
            val dialog = BrowserDialog(activity, host.text(R.string.settings_auto_hide)).custom(preview)
            preview.onSelected = { selected -> prefs.autoHide = selected; dialog.dismiss(); refresh() }
            activity.dialogScope.show(dialog)
        },
        SettingsItem.toggle(3, host.text(R.string.settings_tab_bar), prefs.tabBar && layout in 1..2, updateCheckOnClick = layout in 1..2) { toggleTabs() },
        choice(4, host.text(R.string.settings_address_content), listOf(host.text(R.string.settings_address_title), host.text(R.string.settings_address_url), host.text(R.string.settings_address_domain)), prefs.addressContent) { prefs.addressContent = it },
        toggle(5, host.text(R.string.settings_toolbar_colors), prefs.colorMode, host.text(R.string.settings_toolbar_colors_detail)) { prefs.colorMode = it }
    ) }
    private fun toggleTabs() {
        if (layout !in 1..2) {
            host.activity.dialogScope.show(BrowserDialog(host.activity, host.text(R.string.settings_layout), host.text(R.string.settings_tab_bar_layout))
                .singleChoice(listOf(host.text(R.string.settings_layout_top), host.text(R.string.settings_layout_bottom)), -1) {
                    prefs.toolbarLayout = it + 1; prefs.tabBar = true; host.refresh()
                })
        } else { prefs.tabBar = !prefs.tabBar; host.refresh() }
    }
}
