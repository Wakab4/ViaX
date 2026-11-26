package moe.via.browser.feature.settings

import androidx.annotation.StringRes
import moe.via.browser.R

enum class SettingsRoute(@StringRes val titleRes: Int) {
    ROOT(R.string.ref_menu_settings),
    GENERAL(R.string.settings_general),
    SITE_DATA(R.string.site_data_title),
    PROXY(R.string.proxy_title),
    CUSTOMIZE(R.string.ref_home_design_title),
    PRIVACY(R.string.settings_privacy),
    ADVANCED(R.string.settings_advanced),
    TOOLBAR(R.string.settings_toolbar),
    NIGHT(R.string.ref_menu_night),
    USER_AGENT(R.string.ref_ua_title),
    USER_AGENT_EDITOR(R.string.settings_ua_edit),
    FONT(R.string.ref_font_title),
    OPERATIONS(R.string.settings_operations),
    KEYBOARD(R.string.settings_keyboard),
    ACTIONS(R.string.settings_choose_action),
    LONG_PRESS_MENU(R.string.settings_long_press_menu),
    SSL(R.string.settings_ssl),
    LABS(R.string.settings_labs),
    SKINS(R.string.ref_skin_title),
    WEB_DNS(R.string.web_dns_title),
}
