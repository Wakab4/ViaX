package moe.pure.browser.feature.pageinfo

import moe.pure.browser.ui.message.BrowserMessages

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebView
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.theme.ThemedActivity

class PageDataDialogs(private val activity: Activity) {
    private val dialogs = (activity as? ThemedActivity)?.dialogScope ?: DialogScope()
    fun certificate(view: WebView) {
        dialogs.show(BrowserDialog(activity, "证书信息", CertificateDetails.text(view.certificate)).positive("确定"))
    }
    fun cookies(url: String) {
        val host = Uri.parse(url).host ?: return
        val cookies = CookieManager.getInstance().getCookie(url).orEmpty()
        val dialog = BrowserDialog(activity, "$host 的 Cookies", cookies.ifEmpty { "无 Cookies。" })
        if (cookies.isEmpty()) dialog.positive("确定")
        else dialog.positive("复制") { copy(cookies); true }.negative()
            .neutral("删除") {
                SiteCookies.clear(url) { if (!activity.isDestroyed) BrowserMessages.show(activity, "已置空 $host 的 Cookies") }
                true
            }
        dialogs.show(dialog)
    }
    fun copy(value: String) { activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("文本", value)) }
}
