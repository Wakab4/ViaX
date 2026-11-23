package moe.via.browser.feature.settings.ui

import moe.via.browser.R
import moe.via.browser.core.localization.AppLanguages
import moe.via.browser.ui.dialog.BrowserDialog

internal class LanguageSettingsDialog(private val host: SettingsHost) {
    fun show() {
        val languages = AppLanguages.choices(host.activity)
        val keys = languages.keys.toList()
        host.activity.dialogScope.show(BrowserDialog(host.activity, host.text(R.string.settings_language))
            .singleChoice(languages.values.toList(), keys.indexOf(AppLanguages.selected(host.preferences.language))) { index ->
                if (keys[index] != host.preferences.language) {
                    host.preferences.language = keys[index]
                    host.activity.recreateForSettings()
                }
            })
    }
}
