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

internal enum class PageContextAction(val nativeId: Int, val label: String = "", private val labelResource: Int = 0) {
    BACKGROUND(0, "后台打开"), NEW_TAB(1, "新标签打开"), IMAGE(2, "查看图片"),
    COPY_LINK(3, "复制链接"), SAVE_IMAGE(6, "保存图片"), IMAGE_SEARCH(7, "以图搜图"),
    IMAGE_MODE(8, "看图模式"),
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
    INFO(22, "页面信息"), MARK_AD(23, "标记广告"),
    COPY_TEXT(29, "复制链接文本"), SHARE_LINK(31, "分享"),
    SORT_FAVORITES(32, labelResource = R.string.ref_favorite_context_sort), SHARE_IMAGE(33, "分享图片"),
    SCAN_IMAGE(34, "扫描二维码"), MORE(36, "更多选项"), CUSTOMIZE(37, "定制菜单"),
    DOWNLOAD_IMAGE(38, "下载图片");
    fun label(context: Context) = if (labelResource != 0) context.getString(labelResource) else label
}
