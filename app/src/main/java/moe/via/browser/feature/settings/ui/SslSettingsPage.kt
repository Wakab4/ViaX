package moe.via.browser.feature.settings.ui

import moe.via.browser.R
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.SettingsRoute
import moe.via.browser.ui.dialog.BrowserDialog

internal class SslSettingsPage(private val host: SettingsHost) {
    fun open() {
        if (host.preferences.ignoredSslWarnings != 0) { host.open(SettingsRoute.SSL); return }
        val dialog = BrowserDialog(host.activity, host.text(R.string.settings_ssl), host.text(R.string.settings_ssl_help))
        val acknowledgment = dialog.checkbox(host.text(R.string.settings_ssl_ack))
        host.activity.dialogScope.show(dialog.positive(host.text(android.R.string.ok)) {
            if (acknowledgment.isChecked) { host.open(SettingsRoute.SSL); true } else false
        }.negative())
        val confirm = dialog.findViewById<android.widget.TextView>(R.id.dialog_positive)
        confirm.isEnabled = false; confirm.alpha = .5f
        acknowledgment.setOnCheckedChangeListener { _, checked -> confirm.isEnabled = checked; confirm.alpha = if (checked) 1f else .5f }
    }
    fun rows(): List<SettingsItem> = LABELS.mapIndexed { index, label ->
        val flag = 1 shl index
        SettingsItem.radio(flag, host.text(label), host.preferences.ignoredSslWarnings and flag != 0) {
            host.preferences.ignoredSslWarnings = host.preferences.ignoredSslWarnings xor flag
            host.refresh()
        }
    }
    fun help() = host.message(host.text(R.string.settings_ssl), host.text(R.string.settings_ssl_help))
    companion object {
        private val LABELS = listOf(R.string.settings_ssl_untrusted, R.string.settings_ssl_date,
            R.string.settings_ssl_expired, R.string.settings_ssl_domain, R.string.settings_ssl_future, R.string.settings_ssl_invalid)
    }
}
