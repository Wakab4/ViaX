package moe.via.browser.browser.ui.contextmenu

import android.content.Context
import moe.via.browser.R
import moe.via.browser.ui.dialog.DialogAnchor

data class ContextMenuPageState(
    val enabled: Boolean = true,
    val customTab: Boolean = false,
    val javaScriptEnabled: Boolean = true,
    val adBlockingEnabled: Boolean = true,
    /** u9.d kind: -1 ordinary; generated bookmark/history/about/catalog/saved/folder/blank pages retain their kinds. */
    val internalKind: Int = -1,
)

internal data class ContextMenuTarget(
    val pageUrl: String,
    val pageTitle: String,
    val link: String?,
    val image: String?,
    val linkText: String?,
    val point: DialogAnchor,
    val state: ContextMenuPageState,
)

internal enum class PageContextAction(val nativeId: Int, private val labelResource: Int) {
    BACKGROUND(0, R.string.ref_library_open_background),
    NEW_TAB(1, R.string.ref_library_open_new_tab), IMAGE(2, R.string.settings_context_view_image),
    COPY_LINK(3, R.string.ref_closed_tabs_copy_link), SAVE_IMAGE(6, R.string.settings_context_save_image),
    IMAGE_SEARCH(7, R.string.settings_context_image_search), IMAGE_MODE(8, R.string.settings_context_image_mode),
    EDIT_BOOKMARK(9, labelResource = R.string.ref_bookmark_context_edit),
    ADD_BOOKMARK_FAVORITE(10, labelResource = R.string.ref_bookmark_context_add_favorite),
    DELETE_BOOKMARK(11, labelResource = R.string.ref_generated_history_context_delete),
    EDIT_BOOKMARK_FOLDER(12, labelResource = R.string.ref_bookmark_context_edit),
    DELETE_BOOKMARK_FOLDER(13, labelResource = R.string.ref_generated_history_context_delete),
    EDIT_FAVORITE(14, labelResource = R.string.ref_bookmark_context_edit),
    DELETE_FAVORITE(15, labelResource = R.string.ref_generated_history_context_delete),
    DELETE_HISTORY(16, labelResource = R.string.ref_generated_history_context_delete),
    CLEAR_HISTORY(17, labelResource = R.string.ref_generated_history_context_clear),
    DELETE_SAVED_PAGE(18, labelResource = R.string.ref_generated_history_context_delete),
    INFO(22, R.string.settings_context_page_info), MARK_AD(23, R.string.ref_menu_mark_ad),
    COPY_TEXT(29, R.string.settings_context_copy_link_text), SHARE_LINK(31, R.string.ref_menu_share),
    SORT_FAVORITES(32, labelResource = R.string.ref_favorite_context_sort), SHARE_IMAGE(33, R.string.settings_context_share_image),
    SCAN_IMAGE(34, R.string.ref_menu_scan), MORE(36, R.string.settings_context_more), CUSTOMIZE(37, R.string.ref_menu_customize),
    DOWNLOAD_IMAGE(38, R.string.settings_context_download_image);
    fun label(context: Context) = context.getString(labelResource)
}
