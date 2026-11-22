package moe.via.browser.feature.settings.ui

import moe.via.browser.R
import moe.via.browser.browser.ui.contextmenu.PageContextAction
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.ui.dialog.BrowserDialog

internal class LongPressMenuSettingsPage(private val host: SettingsHost) {
    fun rows(): List<SettingsItem> = ITEMS.map { item ->
        val id = item.nativeId
        SettingsItem.toggle(id, item.label(host.activity), id !in host.preferences.longMenuDisabled) { enabled ->
            host.preferences.longMenuDisabled = if (enabled) host.preferences.longMenuDisabled - id else host.preferences.longMenuDisabled + id
        }
    }
    fun reset() {
        host.activity.dialogScope.show(BrowserDialog(host.activity, host.text(R.string.settings_reset_default),
            host.text(R.string.settings_reset_long_press_confirm))
            .positive(host.text(R.string.ref_menu_reset)) { host.preferences.resetLongMenu(); host.refresh(); true }.negative())
    }
    companion object {
        private val ITEMS = listOf(PageContextAction.BACKGROUND, PageContextAction.NEW_TAB, PageContextAction.IMAGE,
            PageContextAction.SAVE_IMAGE, PageContextAction.DOWNLOAD_IMAGE, PageContextAction.SHARE_IMAGE,
            PageContextAction.IMAGE_SEARCH, PageContextAction.IMAGE_MODE, PageContextAction.INFO, PageContextAction.MARK_AD,
            PageContextAction.COPY_TEXT, PageContextAction.SCAN_IMAGE, PageContextAction.COPY_LINK, PageContextAction.SHARE_LINK)
    }
}
