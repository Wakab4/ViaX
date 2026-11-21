package moe.pure.browser.feature.settings.ui

import moe.pure.browser.feature.settings.SettingsItem
import moe.pure.browser.ui.dialog.BrowserDialog

internal class LongPressMenuSettingsPage(private val host: SettingsHost) {
    fun rows(): List<SettingsItem> = ITEMS.map { (id, title) ->
        SettingsItem.toggle(id, title, id !in host.preferences.longMenuDisabled) { enabled ->
            host.preferences.longMenuDisabled = if (enabled) host.preferences.longMenuDisabled - id else host.preferences.longMenuDisabled + id
        }
    }
    fun reset() {
        host.activity.dialogScope.show(BrowserDialog(host.activity, "重置为默认设置", "你确定继续吗？")
            .positive("重置") { host.preferences.resetLongMenu(); host.refresh(); true }.negative())
    }
    companion object {
        private val ITEMS = listOf(0 to "后台打开", 1 to "新标签打开", 2 to "查看图片", 6 to "保存图片", 38 to "下载图片",
            33 to "分享图片", 7 to "以图搜图", 8 to "看图模式", 22 to "页面信息", 23 to "标记广告",
            29 to "复制链接文本", 34 to "扫描二维码", 3 to "复制链接", 31 to "分享")
    }
}
