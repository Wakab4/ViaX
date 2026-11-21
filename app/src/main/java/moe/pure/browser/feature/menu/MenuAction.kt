package moe.pure.browser.feature.menu

import moe.pure.browser.R

/** Stable IDs are persisted independently from translated labels and drawable names. */
enum class MenuAction(val id: Int, val titleResource: Int, val icon: Int) {
    NIGHT(1, R.string.ref_menu_night, R.drawable.ref_ic_menu_night),
    BOOKMARKS(2, R.string.ref_menu_bookmarks, R.drawable.ref_ic_menu_bookmark),
    HISTORY(3, R.string.ref_menu_history, R.drawable.ref_ic_menu_history),
    DOWNLOADS(4, R.string.ref_menu_downloads, R.drawable.ref_ic_menu_downloads),
    INCOGNITO(5, R.string.ref_menu_incognito, R.drawable.ref_ic_menu_incognito_mode),
    SHARE(6, R.string.ref_menu_share, R.drawable.ref_ic_menu_share),
    ADD_BOOKMARK(7, R.string.ref_menu_add_bookmark, R.drawable.ref_ic_menu_add_bookmark),
    DESKTOP(8, R.string.ref_menu_desktop, R.drawable.ref_ic_menu_desktop_mode),
    TOOLS(9, R.string.ref_menu_tools, R.drawable.ref_ic_menu_tools),
    SETTINGS(10, R.string.ref_menu_settings, R.drawable.ref_ic_menu_settings),
    FIND(11, R.string.ref_menu_find, R.drawable.ref_ic_menu_find),
    SAVE(12, R.string.ref_menu_save, R.drawable.ref_ic_menu_save),
    OFFLINE(13, R.string.ref_menu_offline, R.drawable.ref_ic_menu_offline),
    TRANSLATE(14, R.string.ref_menu_translate, R.drawable.ref_ic_menu_translate),
    SOURCE(15, R.string.ref_menu_source, R.drawable.ref_ic_menu_view_source),
    FULLSCREEN(16, R.string.ref_menu_fullscreen, R.drawable.ref_ic_menu_fullscreen),
    IMAGES(17, R.string.ref_menu_images, R.drawable.ref_ic_menu_image_mode),
    RESOURCES(18, R.string.ref_menu_resources, R.drawable.ref_ic_menu_resource),
    USER_AGENT(19, R.string.ref_menu_user_agent, R.drawable.ref_ic_menu_useragent),
    NETWORK_LOG(20, R.string.ref_menu_network_log, R.drawable.ref_ic_menu_network_log),
    MARK_AD(23, R.string.ref_menu_mark_ad, R.drawable.ref_ic_menu_mark),
    RELOAD(24, R.string.ref_menu_reload, R.drawable.ref_ic_menu_reload),
    SCAN(25, R.string.ref_menu_scan, R.drawable.ref_ic_menu_scan),
    SITE_SETTINGS(26, R.string.ref_menu_site_settings, R.drawable.ref_ic_menu_site_conf),
    SCRIPTS(27, R.string.ref_menu_scripts, R.drawable.ref_ic_menu_scripts),
    ADBLOCK(28, R.string.ref_menu_adblock, R.drawable.ref_ic_menu_adblock),
    FONT_SIZE(29, R.string.ref_menu_font_size, R.drawable.ref_ic_menu_font_size),
    ORIENTATION(30, R.string.ref_menu_orientation, R.drawable.ref_ic_menu_rotation),
    CLEAR_DATA(31, R.string.ref_menu_clear_data, R.drawable.ref_ic_menu_delete),
    PRINT(32, R.string.ref_menu_print, R.drawable.ref_ic_menu_print),
    READ_ALOUD(33, R.string.ref_menu_read_aloud, R.drawable.ref_ic_menu_read_aloud),
    ADD_SHORTCUT(34, R.string.ref_menu_add_shortcut, R.drawable.ref_ic_menu_add_to_home_screen),
    CUSTOMIZE(35, R.string.ref_menu_customize, R.drawable.ref_ic_menu_customize_menu),
    READER(36, R.string.ref_menu_reader, R.drawable.ref_ic_menu_reader),
    OPEN_WITH(37, R.string.ref_menu_open_with, R.drawable.ref_ic_menu_open_with),
    GAME(38, R.string.ref_menu_game, R.drawable.ref_ic_menu_game_mode),
    AI(39, R.string.ref_menu_ai, R.drawable.ref_ic_menu_ai),
    ADD_FAVORITE(40, R.string.ref_menu_add_favorite, R.drawable.ref_ic_menu_add_favorite),
    REPORT(41, R.string.ref_menu_report, R.drawable.ref_ic_menu_report_abuse);

    fun title(context: android.content.Context): String = context.getString(titleResource)

    companion object {
        fun fromId(id: Int): MenuAction? = entries.firstOrNull { it.id == id }
        val defaultOrder = (1..20).toList() + listOf(25, 34, 33, 39, 30, 28, 23, 29, 31, 35)
    }
}
