package moe.via.browser.feature.settings.ui

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
        SettingsItem.content(ToolbarPreviewState::class, preview) { ToolbarPreview(activity, preview.layout, preview.tabs, preview.address) },
        choice(1, "应用布局", listOf("传统", "工具栏在上", "工具栏在下", "双行工具栏"), layout) {
            prefs.toolbarLayout = it
            if (it !in 1..2) prefs.tabBar = false
        },
        SettingsItem.link(2, "自动隐藏操作栏", listOf("总是显示", "滑动来显示/隐藏", "点击来显示/隐藏")[prefs.autoHide.coerceIn(0, 2)]) {
            val preview = AutoHideChoices(activity, prefs.autoHide) { selected -> prefs.autoHide = selected; refresh() }
            val dialog = BrowserDialog(activity, "自动隐藏操作栏").custom(preview)
            preview.onSelected = { selected -> prefs.autoHide = selected; dialog.dismiss(); refresh() }
            activity.dialogScope.show(dialog)
        },
        SettingsItem.toggle(3, "启用标签栏", prefs.tabBar && layout in 1..2) { toggleTabs() },
        choice(4, "地址栏内容", listOf("标题", "地址", "域名"), prefs.addressContent) { prefs.addressContent = it },
        toggle(5, "色彩模式", prefs.colorMode, "浏览网页时操作栏自适应色彩") { prefs.colorMode = it }
    ) }
    private fun toggleTabs() {
        if (!prefs.tabBar && layout !in 1..2) {
            host.activity.dialogScope.show(BrowserDialog(host.activity, "应用布局", "请选择另一种应用布局以启用标签栏")
                .singleChoice(listOf("工具栏在上", "工具栏在下"), -1) {
                    prefs.toolbarLayout = it + 1; prefs.tabBar = true; host.refresh()
                })
        } else { prefs.tabBar = !prefs.tabBar; host.refresh() }
    }
}
