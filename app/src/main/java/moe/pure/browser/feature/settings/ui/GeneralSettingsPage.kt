package moe.pure.browser.feature.settings.ui

import android.os.Build
import moe.pure.browser.feature.adblock.ui.AdblockNavigation
import moe.pure.browser.feature.ai.ui.settings.AiSettingsEntry
import moe.pure.browser.feature.download.DownloadDirectories
import moe.pure.browser.feature.download.DownloadManagers
import moe.pure.browser.feature.menu.customize.MenuCustomizationEntry
import moe.pure.browser.feature.passwords.ui.PasswordSettingsEntry
import moe.pure.browser.feature.reader.ui.ReaderSettingsEntry
import moe.pure.browser.feature.search.ui.settings.SearchSettingsEntry
import moe.pure.browser.feature.settings.SettingsItem
import moe.pure.browser.feature.settings.SettingsRoute
import moe.pure.browser.feature.site.ui.SiteSettingsNavigation
import moe.pure.browser.feature.media.external.ExternalVideoPlayerSettings
import android.content.Intent

internal class GeneralSettingsPage(private val host: SettingsHost) {
    private val activity get() = host.activity
    private val prefs get() = host.preferences
    fun rows(): List<SettingsItem> = with(host) {
        val orientation = listOf("跟随系统", "自动", "竖屏", "横屏")
        val canOrient = Build.VERSION.SDK_INT < 36 || activity.resources.configuration.smallestScreenWidthDp < 600
        listOf(
            SettingsItem.link(26, "同步") { command(SettingsCommand.SYNC) },
            link(2, "浏览器标识", SettingsRoute.USER_AGENT),
            SettingsItem.link(3, "清除数据") { command(SettingsCommand.CLEAR_DATA) },
            SettingsItem.link(4, "广告拦截") { AdblockNavigation.settings(activity) },
            SettingsItem.link(5, "网站设定") { SiteSettingsNavigation.open(activity) },
            SettingsItem.link(36, "密码管理器") { PasswordSettingsEntry.open(activity) },
            link(6, "夜间模式", SettingsRoute.NIGHT),
            SettingsItem.link(30, "阅读模式") { ReaderSettingsEntry.open(activity) },
            link(32, "工具栏设置", SettingsRoute.TOOLBAR),
            SettingsItem.link(28, "定制菜单") { MenuCustomizationEntry.open(activity) },
            link(37, "定制长按菜单", SettingsRoute.LONG_PRESS_MENU),
            SettingsItem.link(9, "语言", LanguageSettingsDialog.label(prefs.language)) { command(SettingsCommand.LANGUAGE) },
            SettingsItem.link(10, "主页", homepageLabel(prefs.homepage)) { command(SettingsCommand.HOMEPAGE) },
            SettingsItem.link(31, "搜索设置") { SearchSettingsEntry.open(activity) },
            SettingsItem.link(34, "AI 设置") { AiSettingsEntry.open(activity) },
            SettingsItem.link(13, "屏幕方向", orientation.getOrElse(if (canOrient) settings.orientation else 0) { orientation[0] }, canOrient) {
                choose("屏幕方向", orientation, settings.orientation) { settings.orientation = it; settings.applyOrientation(activity) }
            },
            SettingsItem.link(14, "下载目录", DownloadDirectories(activity).summary()) { command(SettingsCommand.DOWNLOAD_DIRECTORY) },
            SettingsItem.link(15, "下载管理", DownloadManagers(activity).label()) { command(SettingsCommand.DOWNLOAD_MANAGER) },
            SettingsItem.link(27, "外置视频播放器", ExternalVideoPlayerSettings(activity, { prefs.externalVideoPlayer }, { prefs.externalVideoPlayer = it }).label()) { command(SettingsCommand.EXTERNAL_PLAYER) },
            SettingsItem.link(18, "退出时清除数据") { command(SettingsCommand.EXIT_CLEAR) },
            link(35, "字体", SettingsRoute.FONT, prefs.uiFont),
            link(19, "操作设定", SettingsRoute.OPERATIONS, "手势，快捷方式，功能键"),
            SettingsItem.link(20, "导入/备份书签") { command(SettingsCommand.BOOKMARK_DOCUMENTS) },
            SettingsItem.link(21, "导入数据") { command(SettingsCommand.IMPORT_DATA) },
            SettingsItem.link(22, "导出数据", "导出书签、设置、脚本和广告规则") { command(SettingsCommand.EXPORT_DATA) },
            choice(23, "启动时恢复未关闭标签", listOf("禁用恢复", "总是恢复", "优先询问"), prefs.restoreTabs) { prefs.restoreTabs = it },
            toggle(33, "显示撤销关闭标签的提示", prefs.undoClosePrompt, "如果开启了隐身模式，则不会显示提示") { prefs.undoClosePrompt = it },
            SettingsItem.link(24, "设置默认浏览器") { command(SettingsCommand.DEFAULT_BROWSER) }
        )
    }
    private fun homepageLabel(url: String) = when (url) {
        "about:home" -> "默认"; "about:blank" -> "空白页"; "about:bookmarks" -> "书签"; else -> url
    }
}
