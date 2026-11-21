package moe.pure.browser.feature.passwords.ui

import moe.pure.browser.ui.theme.ThemedActivity

internal object PasswordSettingsEntry {
    fun open(activity: ThemedActivity) { activity.currentRecordStack?.push(PasswordSettingsFragment::class.java, null) }
}
