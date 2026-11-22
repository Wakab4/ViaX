package moe.via.browser.feature.network

import moe.via.browser.ui.message.BrowserMessages

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.webkit.WebView
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.feature.adblock.CustomBlockRules
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.browser.page.WebTouchPoint

class NetworkContextMenu(private val activity: Activity, private val rules: CustomBlockRules,
    private val actions: Actions) {
    private val dialogs = (activity as? ThemedActivity)?.dialogScope ?: DialogScope()
    interface Actions {
        fun open(url: String, selected: Boolean)
        fun download(url: String)
        fun share(url: String)
        fun play(url: String)
        fun clear(tab: BrowserTab)
    }
    fun show(tab: BrowserTab, view: WebView): Boolean {
        if (!NetworkLogPages.isPage(activity, tab.url)) return false
        val hit = view.hitTestResult
        if (hit.type !in setOf(WebView.HitTestResult.SRC_ANCHOR_TYPE, WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) return false
        val url = hit.extra ?: return false
        if (Uri.parse(url).scheme !in setOf("http", "https")) return false
        val choices = mutableListOf<Pair<String, () -> Unit>>()
        choices += "后台打开" to { actions.open(url, false) }
        choices += "新标签打开" to { actions.open(url, true) }
        val resources = Uri.parse(tab.url).lastPathSegment == "res.html"
        if (!resources) {
            val domain = rules.contains(url, true)
            val link = rules.contains(url, false)
            choices += (if (domain) "放行域名" else "拦截域名") to { changeRule(url, true, !domain) }
            choices += (if (link) "放行链接" else "拦截链接") to { changeRule(url, false, !link) }
        } else choices += "通过应用播放…" to { actions.play(url) }
        choices += "下载" to { actions.download(url) }
        choices += "清空" to {
            dialogs.show(BrowserDialog(activity, "清空", "你确定继续吗？").positive("确定") { actions.clear(tab); true }.negative())
        }
        choices += "复制链接" to {
            val value = if (resources) url else url.substringAfter("://")
            activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("链接", value))
            toast("已复制链接")
        }
        choices += "分享" to { actions.share(url) }
        val dialog = BrowserDialog(activity).items(choices.map { it.first }) { index -> choices[index].second() }
        WebTouchPoint.last(view)?.let { dialog.anchoredAt(it.x, it.y) }
        dialogs.show(dialog)
        return true
    }
    private fun changeRule(url: String, domain: Boolean, blocked: Boolean) {
        BrowserExecutors.persistence.execute {
            val result = runCatching { rules.set(url, domain, blocked) }
            activity.runOnUiThread {
                if (!activity.isDestroyed) toast(if (result.isFailure) "操作失败" else
                    (if (blocked) "拦截了：" else "放行了：") + (if (domain) Uri.parse(url).host else url.substringAfter("://")) + "\n刷新网页生效。")
            }
        }
    }
    private fun toast(value: String) = BrowserMessages.show(activity, value)
}
