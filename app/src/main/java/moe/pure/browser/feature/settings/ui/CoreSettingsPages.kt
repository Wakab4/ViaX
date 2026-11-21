package moe.pure.browser.feature.settings.ui

import android.os.Build
import moe.pure.browser.feature.settings.SettingsItem
import moe.pure.browser.feature.settings.SettingsRoute
import moe.pure.browser.feature.home.data.HomePreferences

internal class CoreSettingsPages(private val host: SettingsHost) {
    private val prefs get() = host.preferences
    fun root(): List<SettingsItem> = with(host) {
        buildList {
            add(link(1, "通用", SettingsRoute.GENERAL))
            add(SettingsItem.link(8, "定制") { command(SettingsCommand.CUSTOMIZE) })
            add(link(2, "隐私", SettingsRoute.PRIVACY)); add(link(3, "高级", SettingsRoute.ADVANCED))
            add(SettingsItem.link(9, "脚本") { command(SettingsCommand.SCRIPTS) })
            if (prefs.labsEnabled) add(link(12, "实验", SettingsRoute.LABS))
            add(SettingsItem.link(5, "关于") { command(SettingsCommand.ABOUT) })
        }
    }
    fun privacy(): List<SettingsItem> = with(host) { listOf(
        toggle(1, "不跟踪", prefs.doNotTrack) { prefs.doNotTrack = it },
        toggle(6, "禁用 WebRTC", prefs.disableWebRtc) { prefs.disableWebRtc = it },
        toggle(4, "不出售或分享数据", prefs.globalPrivacyControl, "请求网站不出售或分享我的数据以保护隐私 (实验性)") { prefs.globalPrivacyControl = it }
    ) }
    fun advanced(): List<SettingsItem> = with(host) { buildList {
        add(toggle(4, "流量节省", prefs.saveData, "请求网站节省流量") { prefs.saveData = it })
        add(toggle(1, "自动展示嗅探按钮", prefs.autoSniff) { prefs.autoSniff = it })
        add(toggle(3, "允许调试网页", prefs.webDebugging) {
            prefs.webDebugging = it
            if (it) message("允许调试网页", "现在你可以将手机连上电脑，然后在 Chrome 电脑版上通过 \"chrome://inspect\" 来调试网页。别忘了先开启设置中开发者选项里的 USB 调试。")
        })
        if (Build.VERSION.SDK_INT >= 33) add(toggle(8, "禁用预测返回手势", prefs.predictiveBackDisabled) { prefs.predictiveBackDisabled = it })
        add(toggle(7, "禁用 Custom Tabs", prefs.customTabsDisabled, "跳过第三方应用通过 Custom Tabs 打开链接时的中转页") { prefs.customTabsDisabled = it })
        add(toggle(5, "禁用安全浏览", prefs.safeBrowsingDisabled, "安全浏览允许 WebView 通过验证链接来防止恶意软件和网络钓鱼攻击") { prefs.safeBrowsingDisabled = it })
        add(SettingsItem.link(6, "忽略 SSL 证书警告", "请小心，忽略 SSL 证书警告可能会使网站的连接不安全") { command(SettingsCommand.SSL) })
    } }
    fun labs(): List<SettingsItem> = with(host) { buildList {
        add(link(2, "皮肤", SettingsRoute.SKINS))
        if (Build.VERSION.SDK_INT >= 31) add(toggle(1, "模糊效果", prefs.blurEnabled, "为应用启用模糊效果，部分设备可能不支持") { prefs.blurEnabled = it })
        if (Build.VERSION.SDK_INT >= 29) add(toggle(3, "在设置中显示背景", prefs.settingsBackground) {
            prefs.settingsBackground = it
            message("在设置中显示背景", "将在下次打开设置时生效")
        })
    } }
    fun homepage() {
        val labels = listOf("默认", "空白页", "书签", "网页")
        val urls = listOf("about:home", "about:blank", "about:bookmarks")
        val selected = urls.indexOf(prefs.homepage).takeIf { it >= 0 } ?: 3
        host.choose("主页", labels, selected) { index ->
            if (index < 3) setHomepage(urls[index]) else customHomepage()
        }
    }
    private fun setHomepage(url: String) { HomePreferences(host.activity).homepage = url; host.refresh() }
    private fun customHomepage() {
        val dialog = moe.pure.browser.ui.dialog.BrowserDialog(host.activity, "自定义主页")
        val field = dialog.field(prefs.homepage.takeUnless { it.startsWith("about:") }.orEmpty(), "自定义主页", 3)
        host.activity.dialogScope.show(dialog.positive("确定") {
            val text = field.text.toString().trim()
            val resolved = moe.pure.browser.feature.search.resolve.SearchUrlResolver.resolve(text, "https://www.google.com/search?q=%s")?.url.orEmpty()
            if (resolved.contains("://")) { setHomepage(resolved); true } else false
        }.negative())
    }
}
