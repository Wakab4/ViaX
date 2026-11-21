package moe.pure.browser.feature.site.ui.image

import android.app.Activity
import moe.pure.browser.R
import moe.pure.browser.feature.site.ui.redirect.RedirectSettingsDomain
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope

internal class ImageSettingsDialogs(private val activity: Activity) : AutoCloseable {
    private val scope = DialogScope()
    private var current: BrowserDialog? = null

    fun mode(enabled: Boolean, wifiOnly: Boolean, save: (Int) -> Unit) {
        val original = if (wifiOnly) 2 else if (enabled) 0 else 1
        show(BrowserDialog(activity, activity.getString(R.string.ref_image_page_title)).singleChoice(listOf(
            activity.getString(R.string.ref_image_page_mode_show), activity.getString(R.string.ref_image_page_mode_block),
            activity.getString(R.string.ref_image_page_mode_wifi)), original) { if (it != original) save(it) })
    }

    fun add(globalEnabled: () -> Boolean, save: (String, Boolean) -> Unit) {
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_image_page_add), activity.getString(
            if (globalEnabled()) R.string.ref_image_page_add_block else R.string.ref_image_page_add_allow))
        val input = dialog.field("", "www.example.com")
        show(dialog.positive(activity.getString(android.R.string.ok)) {
            RedirectSettingsDomain.input(input.text.toString())?.let(RedirectSettingsDomain::normalize)
                ?.takeIf(String::isNotEmpty)?.let { key -> save(key, !globalEnabled()) }
            true
        }.negative(activity.getString(android.R.string.cancel)))
    }

    fun edit(key: String, allowed: Boolean, save: (Boolean) -> Unit, remove: () -> Unit) {
        if (key.isEmpty()) return
        show(BrowserDialog(activity, RedirectSettingsDomain.display(key)).singleChoice(listOf(
            activity.getString(R.string.ref_image_page_allow), activity.getString(R.string.ref_image_page_block)),
            if (allowed) 0 else 1) { save(it == 0) }.positive(activity.getString(android.R.string.ok))
            .neutral(activity.getString(R.string.ref_image_page_delete)) { remove(); true })
    }

    private fun show(dialog: BrowserDialog) {
        current?.dismiss(); current = dialog
        scope.show(dialog) { if (current === dialog) current = null }
    }
    fun back(): Boolean { val dialog = current ?: return false; dialog.dismiss(); return true }
    override fun close() { scope.dispose(); current = null }
}
