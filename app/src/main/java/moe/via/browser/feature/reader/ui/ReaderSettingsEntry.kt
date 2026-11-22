package moe.via.browser.feature.reader.ui

import moe.via.browser.ui.theme.ThemedActivity

internal object ReaderSettingsEntry {
    fun open(activity: ThemedActivity) { activity.currentRecordStack?.push(ReaderSettingsFragment::class.java, null) }
}
