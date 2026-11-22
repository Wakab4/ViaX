package moe.via.browser.feature.settings.ui

import moe.via.browser.R
import android.content.Intent
import moe.via.browser.BrowserActivity
import moe.via.browser.feature.library.history.HistoryDirtyState
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
                    moe.via.browser.feature.internalpages.GeneratedPageDirtyState.markAll()
                    HistoryDirtyState.persist(host.activity.applicationContext)
                    host.refresh()
                    host.activity.dialogScope.show(BrowserDialog(host.activity, host.text(R.string.settings_language), host.text(R.string.settings_restart_language))
                        .positive(host.text(R.string.settings_restart)) { restart(); true }.negative())
                }
            })
    }
    private fun restart() {
        host.activity.startActivity(Intent(host.activity, BrowserActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        host.activity.finish()
    }
}
