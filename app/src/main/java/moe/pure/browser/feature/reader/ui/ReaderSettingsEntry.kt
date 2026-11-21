package moe.pure.browser.feature.reader.ui

import moe.pure.browser.ui.theme.ThemedActivity

internal object ReaderSettingsEntry {
    fun open(activity: ThemedActivity) { activity.currentRecordStack?.push(ReaderSettingsFragment::class.java, null) }
}
