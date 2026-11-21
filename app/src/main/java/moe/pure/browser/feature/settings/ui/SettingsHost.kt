package moe.pure.browser.feature.settings.ui

import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.settings.SettingsItem
import moe.pure.browser.feature.settings.SettingsRoute
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.theme.ThemedActivity

/** Page providers describe values; their Fragment supplies navigation and document dispatch. */
internal class SettingsHost(
    val activity: ThemedActivity,
    val settings: BrowserSettings,
    val open: (SettingsRoute) -> Unit,
    val refresh: () -> Unit,
    val command: (SettingsCommand) -> Unit
) {
    val preferences get() = settings.preferences
    fun link(id: Int, title: String, next: SettingsRoute, detail: String = "") = SettingsItem.link(id, title, detail) { open(next) }
    fun choose(title: String, labels: List<String>, selected: Int, changed: (Int) -> Unit) {
        activity.dialogScope.show(BrowserDialog(activity, title).singleChoice(labels, selected) { changed(it); refresh() })
    }
    fun choice(id: Int, title: String, labels: List<String>, selected: Int, changed: (Int) -> Unit): SettingsItem =
        SettingsItem.link(id, title, labels.getOrElse(selected) { labels.firstOrNull().orEmpty() }) { choose(title, labels, selected, changed) }
    fun toggle(id: Int, title: String, checked: Boolean, detail: String = "", changed: (Boolean) -> Unit): SettingsItem =
        SettingsItem.toggle(id, title, checked, detail) { changed(it) }
    fun message(title: String, text: String) {
        activity.dialogScope.show(BrowserDialog(activity, title, text).positive("确定"))
    }
}

internal enum class SettingsCommand {
    CUSTOMIZE, SCRIPTS, ABOUT, SYNC, DOWNLOAD_DIRECTORY, DOWNLOAD_MANAGER,
    EXTERNAL_PLAYER, BOOKMARK_DOCUMENTS, IMPORT_DATA, EXPORT_DATA, DEFAULT_BROWSER,
    CLEAR_DATA, EXIT_CLEAR, HOMEPAGE, LANGUAGE, SSL, ACTION_CHOSEN
}
