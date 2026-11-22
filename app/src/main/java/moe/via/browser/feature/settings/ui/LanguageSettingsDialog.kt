package moe.via.browser.feature.settings.ui

import android.content.Intent
import moe.via.browser.BrowserActivity
import moe.via.browser.feature.library.history.HistoryDirtyState
import moe.via.browser.core.localization.AppLanguages
import moe.via.browser.ui.dialog.BrowserDialog

internal class LanguageSettingsDialog(private val host: SettingsHost) {
    fun show() {
        val languages = AppLanguages.choices
        val keys = languages.keys.toList()
        host.activity.dialogScope.show(BrowserDialog(host.activity, "语言")
            .singleChoice(languages.values.toList(), keys.indexOf(AppLanguages.selected(host.preferences.language))) { index ->
                if (keys[index] != host.preferences.language) {
                    host.preferences.language = keys[index]
                    moe.via.browser.feature.internalpages.GeneratedPageDirtyState.markAll()
                    HistoryDirtyState.persist(host.activity.applicationContext)
                    host.refresh()
                    host.activity.dialogScope.show(BrowserDialog(host.activity, "语言", "这将在下次启动应用时生效，你希望立即重启应用以生效吗？")
                        .positive("重启应用") { restart(); true }.negative())
                }
            })
    }
    private fun restart() {
        host.activity.startActivity(Intent(host.activity, BrowserActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        host.activity.finish()
    }
    companion object {
        fun label(tag: String): String = AppLanguages.label(tag)
    }
}
