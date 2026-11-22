package moe.via.browser.feature.useragent

import android.app.Activity
import moe.via.browser.R
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.useragent.ui.UserAgentLabels
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.feature.useragent.data.UserAgentRepository

/** s6.Pa/O3 quick selector. The host performs policy refresh and Native conditional reload. */
class UserAgentDialogs(private val activity: Activity, private val repository: UserAgentRepository, private val settings: BrowserSettings) {
    private val dialogs = (activity as? ThemedActivity)?.dialogScope ?: DialogScope()
    fun choose(onChanged: () -> Unit) {
        val desktop = settings.preferences.desktop
        val entries = repository.all().filter { !desktop || it.id >= 0 || it.id in -5..-3 }
        val selected = if (desktop) settings.preferences.desktopUserAgentId else settings.userAgentId
        val labels = entries.map { UserAgentLabels.label(activity, it) }
        dialogs.show(BrowserDialog(activity, activity.getString(if (desktop) R.string.ref_ua_desktop_title else R.string.ref_ua_title))
            .singleChoice(labels, entries.indexOfFirst { it.id == selected }.coerceAtLeast(0)) { index ->
                val value = entries[index]
                if (value.id != selected) {
                    UserAgentPreferences(settings).let { if (desktop) it.desktop(value) else it.global(value) }
                    onChanged()
                    BrowserMessage(activity).show(activity.getString(R.string.ref_page_tool_changed,
                        activity.getString(R.string.ref_ua_title), labels[index]))
                }
            })
    }
}