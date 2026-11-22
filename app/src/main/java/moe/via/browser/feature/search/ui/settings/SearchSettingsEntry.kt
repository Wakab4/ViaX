package moe.via.browser.feature.search.ui.settings

import moe.via.browser.ui.theme.ThemedActivity

internal object SearchSettingsEntry {
    fun open(activity: ThemedActivity) { activity.currentRecordStack?.push(SearchSettingsFragment::class.java, null) }
}
