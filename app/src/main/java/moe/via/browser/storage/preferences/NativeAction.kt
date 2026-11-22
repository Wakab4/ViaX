package moe.via.browser.storage.preferences

enum class NativeAction(val id: Int, val label: String) {
    NONE(0, "无操作"), RELOAD(1, "刷新网页"), TOP(2, "返回顶部"), BOTTOM(3, "滑到底部"),
    ADDRESS(4, "输入网址"), NEW_TAB(5, "新建标签"), ADD_BOOKMARK(6, "添加书签"),
    BOOKMARKS(7, "打开书签"), HISTORY(8, "打开历史"), CLOSE_TAB(9, "关闭标签"),
    PREVIOUS_TAB(10, "上一个标签"), NEXT_TAB(11, "下一个标签"), BACK(12, "网页后退"),
    FORWARD(13, "网页前进"), FIND(14, "页内查找"), TRANSLATE(15, "翻译"),
    CLOSE_ALL(16, "关闭所有标签页"), PAGE_UP(17, "向上翻页"), PAGE_DOWN(18, "向下翻页"),
    SAVE(19, "保存网页"), SOURCE(20, "源码"), READ_ALOUD(24, "朗读网页"), READER(25, "阅读模式"),
    SETTINGS(26, "打开设置"), DUPLICATE(27, "复制标签页"), DOWNLOADS(28, "打开下载"),
    AI(29, "打开 AI 面板"), BOOKMARK_DIALOG(30, "打开书签对话框");

    companion object { fun fromId(id: Int) = entries.firstOrNull { it.id == id } ?: NONE }
}

enum class ActionBinding(val key: String, val label: String, val defaultAction: NativeAction) {
    LONG_PRESS_BACK("keyback", "长按后退", NativeAction.TOP),
    LONG_PRESS_FORWARD("keyforward", "长按前进", NativeAction.BOTTOM),
    LONG_PRESS_HOME("keyhome", "长按主页", NativeAction.ADDRESS),
    LONG_PRESS_TAB("keytab", "长按标签", NativeAction.NEW_TAB),
    LONG_PRESS_MENU("keymenu", "长按菜单", NativeAction.RELOAD),
    TOOLBAR_LEFT("gesturetoolbarleft", "工具栏左滑", NativeAction.PREVIOUS_TAB),
    TOOLBAR_RIGHT("gesturetoolbarright", "工具栏右滑", NativeAction.NEXT_TAB)
}
