package moe.via.browser.feature.settings.ui

import moe.via.browser.R
import moe.via.browser.ui.theme.window.applyActivityOrientation

import android.os.Build
import moe.via.browser.core.localization.AppLanguages
import moe.via.browser.feature.adblock.ui.AdblockNavigation
import moe.via.browser.feature.ai.ui.settings.AiSettingsEntry
import moe.via.browser.feature.download.directory.DownloadDirectories
import moe.via.browser.feature.download.DownloadManagers
import moe.via.browser.browser.ui.menu.customize.MenuCustomizationEntry
import moe.via.browser.feature.passwords.ui.PasswordSettingsEntry
import moe.via.browser.feature.reader.ui.ReaderSettingsEntry
import moe.via.browser.feature.search.ui.settings.SearchSettingsEntry
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.SettingsRoute
import moe.via.browser.feature.site.ui.SiteSettingsNavigation
import moe.via.browser.feature.media.external.ExternalVideoPlayerSettings
import android.content.Intent

internal class GeneralSettingsPage(private val host: SettingsHost) {
    private val activity get() = host.activity
    private val prefs get() = host.preferences
    fun rows(): List<SettingsItem> = with(host) {
        val orientation = listOf(host.text(R.string.settings_follow_system), host.text(R.string.settings_automatic), host.text(R.string.settings_portrait), host.text(R.string.settings_landscape))
        val canOrient = Build.VERSION.SDK_INT < 36 || activity.resources.configuration.smallestScreenWidthDp < 600
        listOf(
            SettingsItem.section(host.text(R.string.settings_group_common)),
            SettingsItem.link(9, host.text(R.string.settings_language), AppLanguages.label(activity, prefs.language)) { command(SettingsCommand.LANGUAGE) },
            SettingsItem.link(10, host.text(R.string.ref_home_page_title), homepageLabel(prefs.homepage)) { command(SettingsCommand.HOMEPAGE) },
            SettingsItem.link(31, host.text(R.string.ref_search_page_title)) { SearchSettingsEntry.open(activity) },
            SettingsItem.link(24, host.text(R.string.settings_make_default)) { command(SettingsCommand.DEFAULT_BROWSER) },

            SettingsItem.section(host.text(R.string.settings_group_display)),
            link(6, host.text(R.string.ref_menu_night), SettingsRoute.NIGHT),
            SettingsItem.link(30, host.text(R.string.ref_reader_settings_title)) { ReaderSettingsEntry.open(activity) },
            link(35, host.text(R.string.ref_font_title), SettingsRoute.FONT, prefs.uiFont),
            SettingsItem.link(13, host.text(R.string.settings_orientation), orientation.getOrElse(if (canOrient) settings.orientation else 0) { orientation[0] }, canOrient) {
                choose(host.text(R.string.settings_orientation), orientation, settings.orientation) { settings.orientation = it; applyActivityOrientation(activity, it) }
            },

            SettingsItem.section(host.text(R.string.settings_group_controls_tabs)),
            link(32, host.text(R.string.settings_toolbar), SettingsRoute.TOOLBAR),
            SettingsItem.link(28, host.text(R.string.settings_menu_customize)) { MenuCustomizationEntry.open(activity) },
            link(37, host.text(R.string.settings_long_press_menu), SettingsRoute.LONG_PRESS_MENU),
            link(19, host.text(R.string.settings_operations), SettingsRoute.OPERATIONS, host.text(R.string.settings_controls_detail)),
            choice(23, host.text(R.string.ref_closed_tabs_restore_setting), listOf(host.text(R.string.ref_closed_tabs_restore_0), host.text(R.string.ref_closed_tabs_restore_1), host.text(R.string.ref_closed_tabs_restore_2)), prefs.restoreTabs) { prefs.restoreTabs = it },
            toggle(33, host.text(R.string.ref_closed_tabs_undo_prompt), prefs.undoClosePrompt, host.text(R.string.ref_closed_tabs_undo_prompt_description)) { prefs.undoClosePrompt = it },

            SettingsItem.section(host.text(R.string.settings_group_webpages)),
            SettingsItem.link(5, host.text(R.string.ref_site_editor_title)) { SiteSettingsNavigation.open(activity) },
            SettingsItem.link(4, host.text(R.string.ref_adblock_page_title)) { AdblockNavigation.settings(activity) },
            link(2, host.text(R.string.ref_ua_title), SettingsRoute.USER_AGENT),

            SettingsItem.section(host.text(R.string.settings_group_tools)),
            SettingsItem.link(14, host.text(R.string.ref_download_directory_title), DownloadDirectories(activity).summary()) { command(SettingsCommand.DOWNLOAD_DIRECTORY) },
            SettingsItem.link(15, host.text(R.string.settings_download_manager), DownloadManagers(activity).label()) { command(SettingsCommand.DOWNLOAD_MANAGER) },
            SettingsItem.link(27, host.text(R.string.settings_player), ExternalVideoPlayerSettings(activity, { prefs.externalVideoPlayer }, { prefs.externalVideoPlayer = it }).label()) { command(SettingsCommand.EXTERNAL_PLAYER) },
            SettingsItem.link(34, host.text(R.string.ref_ai_page_settings)) { AiSettingsEntry.open(activity) },

            SettingsItem.section(host.text(R.string.settings_group_data_sync)),
            SettingsItem.link(26, host.text(R.string.settings_sync)) { command(SettingsCommand.SYNC) },
            SettingsItem.link(36, host.text(R.string.ref_password_csv_settings_title)) { PasswordSettingsEntry.open(activity) },
            SettingsItem.link(20, host.text(R.string.settings_bookmark_documents)) { command(SettingsCommand.BOOKMARK_DOCUMENTS) },
            SettingsItem.link(21, host.text(R.string.settings_data_import)) { command(SettingsCommand.IMPORT_DATA) },
            SettingsItem.link(22, host.text(R.string.settings_data_export), host.text(R.string.settings_data_export_detail)) { command(SettingsCommand.EXPORT_DATA) },
            SettingsItem.link(3, host.text(R.string.ref_menu_clear_data)) { command(SettingsCommand.CLEAR_DATA) },
            SettingsItem.link(18, host.text(R.string.settings_clear_exit)) { command(SettingsCommand.EXIT_CLEAR) },
        )
    }
    private fun homepageLabel(url: String) = when (url) {
        "about:home" -> host.text(R.string.ref_font_default); "about:blank" -> host.text(R.string.ref_blank_page_title); "about:bookmarks" -> host.text(R.string.ref_menu_bookmarks); else -> url
    }
}
