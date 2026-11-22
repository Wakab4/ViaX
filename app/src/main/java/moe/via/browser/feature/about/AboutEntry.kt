package moe.via.browser.feature.about

import moe.via.browser.ui.theme.ThemedActivity

internal object AboutEntry {
    fun open(activity: ThemedActivity) { activity.currentRecordStack?.push(AboutFragment::class.java, null) }
}
