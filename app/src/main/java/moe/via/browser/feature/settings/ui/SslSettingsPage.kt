package moe.via.browser.feature.settings.ui

import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.SettingsRoute
import moe.via.browser.ui.dialog.BrowserDialog

internal class SslSettingsPage(private val host: SettingsHost) {
    fun open() {
        if (host.preferences.ignoredSslWarnings != 0) { host.open(SettingsRoute.SSL); return }
        val dialog = BrowserDialog(host.activity, "忽略 SSL 证书警告", HELP)
        val acknowledgment = dialog.checkbox("我了解这可能导致连接不安全")
        host.activity.dialogScope.show(dialog.positive("确定") {
            if (acknowledgment.isChecked) { host.open(SettingsRoute.SSL); true } else false
        }.negative())
        val confirm = dialog.findViewById<android.widget.TextView>(moe.via.browser.R.id.dialog_positive)
        confirm.isEnabled = false; confirm.alpha = .5f
        acknowledgment.setOnCheckedChangeListener { _, checked -> confirm.isEnabled = checked; confirm.alpha = if (checked) 1f else .5f }
    }
    fun rows(): List<SettingsItem> = LABELS.mapIndexed { index, label ->
        val flag = 1 shl index
        SettingsItem.radio(flag, label, host.preferences.ignoredSslWarnings and flag != 0) {
            host.preferences.ignoredSslWarnings = host.preferences.ignoredSslWarnings xor flag
            host.refresh()
        }
    }
    fun help() = host.message("忽略 SSL 证书警告", HELP)
    companion object {
        private val LABELS = listOf("证书不受信任", "证书日期无效", "证书已过期", "证书域名与网站域名不匹配", "证书尚未生效", "证书无效")
        private const val HELP = "SSL 证书用于验证网站身份和加密连接。这就像是把信放在信封中再寄出，只有你和网站可以拆开这封信。\n\n一旦你忽略 SSL 警告，则与网站的连接可能不安全，这意味着与网站传输的数据可能被人窃取。这就像把信放在一个破损的信封上然后寄出它，伪造证书的人可以拆开它。"
    }
}
