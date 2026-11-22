package moe.via.browser.feature.site.ui.redirect

import android.app.Activity
import moe.via.browser.R
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope

internal class RedirectSettingsDialogs(private val activity: Activity) : AutoCloseable {
    private val scope = DialogScope()
    private var current: BrowserDialog? = null

    fun add(globalAllowed: Boolean, save: (String, Boolean) -> Unit) {
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_redirect_page_add), activity.getString(
            if (globalAllowed) R.string.ref_redirect_page_add_ask else R.string.ref_redirect_page_add_allow))
        val input = dialog.field("", "www.example.com")
        show(dialog.positive(activity.getString(android.R.string.ok)) {
            RedirectSettingsDomain.input(input.text.toString())?.let { key -> save(key, !globalAllowed) }
            true
        }.negative(activity.getString(android.R.string.cancel)))
    }

    fun edit(key: String, allowed: Boolean, save: (Boolean) -> Unit, remove: () -> Unit) {
        if (key.isEmpty()) return
        show(BrowserDialog(activity, RedirectSettingsDomain.display(key)).singleChoice(listOf(
            activity.getString(R.string.ref_redirect_setting_allow), activity.getString(R.string.ref_redirect_setting_ask)),
            if (allowed) 0 else 1) { save(it == 0) }
            .positive(activity.getString(android.R.string.ok))
            .neutral(activity.getString(R.string.ref_redirect_page_delete)) { remove(); true })
    }

    private fun show(dialog: BrowserDialog) {
        current?.dismiss(); current = dialog
        scope.show(dialog) { if (current === dialog) current = null }
    }
    fun back(): Boolean { val dialog = current ?: return false; dialog.dismiss(); return true }
    override fun close() { scope.dispose(); current = null }
}
