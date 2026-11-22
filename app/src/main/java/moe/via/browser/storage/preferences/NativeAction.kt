package moe.via.browser.storage.preferences

import androidx.annotation.StringRes
import moe.via.browser.R

enum class NativeAction(val id: Int, @StringRes val labelRes: Int) {
    NONE(0, R.string.settings_action_none), RELOAD(1, R.string.ref_menu_reload), TOP(2, R.string.settings_action_top), BOTTOM(3, R.string.settings_action_bottom),
    ADDRESS(4, R.string.settings_action_address), NEW_TAB(5, R.string.ref_tab_switcher_new_tab), ADD_BOOKMARK(6, R.string.ref_menu_add_bookmark),
    BOOKMARKS(7, R.string.ref_menu_bookmarks), HISTORY(8, R.string.ref_menu_history), CLOSE_TAB(9, R.string.settings_action_close_tab),
    PREVIOUS_TAB(10, R.string.settings_action_previous), NEXT_TAB(11, R.string.settings_action_next), BACK(12, R.string.settings_action_back),
    FORWARD(13, R.string.settings_action_forward), FIND(14, R.string.ref_menu_find), TRANSLATE(15, R.string.ref_menu_translate),
    CLOSE_ALL(16, R.string.ref_tab_switcher_close_all), PAGE_UP(17, R.string.settings_action_page_up), PAGE_DOWN(18, R.string.settings_action_page_down),
    SAVE(19, R.string.ref_saved_save_title), SOURCE(20, R.string.ref_menu_source), READ_ALOUD(24, R.string.ref_menu_read_aloud), READER(25, R.string.ref_menu_reader),
    SETTINGS(26, R.string.ref_menu_settings), DUPLICATE(27, R.string.ref_tab_switcher_duplicate), DOWNLOADS(28, R.string.ref_menu_downloads),
    AI(29, R.string.ref_menu_ai), BOOKMARK_DIALOG(30, R.string.settings_action_bookmark_dialog);

    companion object { fun fromId(id: Int) = entries.firstOrNull { it.id == id } ?: NONE }
}

enum class ActionBinding(val key: String, @StringRes val labelRes: Int, val defaultAction: NativeAction) {
    LONG_PRESS_BACK("keyback", R.string.settings_binding_back, NativeAction.TOP),
    LONG_PRESS_FORWARD("keyforward", R.string.settings_binding_forward, NativeAction.BOTTOM),
    LONG_PRESS_HOME("keyhome", R.string.settings_binding_home, NativeAction.ADDRESS),
    LONG_PRESS_TAB("keytab", R.string.settings_binding_tab, NativeAction.NEW_TAB),
    LONG_PRESS_MENU("keymenu", R.string.settings_binding_menu, NativeAction.RELOAD),
    TOOLBAR_LEFT("gesturetoolbarleft", R.string.settings_binding_left, NativeAction.PREVIOUS_TAB),
    TOOLBAR_RIGHT("gesturetoolbarright", R.string.settings_binding_right, NativeAction.NEXT_TAB)
}
