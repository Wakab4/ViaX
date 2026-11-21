package moe.pure.browser.feature.search.ui.settings

import moe.pure.browser.ui.theme.ThemedActivity

internal object SearchSettingsEntry {
    fun open(activity: ThemedActivity) { activity.currentRecordStack?.push(SearchSettingsFragment::class.java, null) }
}
