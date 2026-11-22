package moe.via.browser.feature.passwords.ui

import moe.via.browser.ui.theme.ThemedActivity

internal object PasswordSettingsEntry {
    fun open(activity: ThemedActivity) { activity.currentRecordStack?.push(PasswordSettingsFragment::class.java, null) }
}
