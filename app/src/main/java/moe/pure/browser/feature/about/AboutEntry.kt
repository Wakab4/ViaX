package moe.pure.browser.feature.about

import moe.pure.browser.ui.theme.ThemedActivity

internal object AboutEntry {
    fun open(activity: ThemedActivity) { activity.currentRecordStack?.push(AboutFragment::class.java, null) }
}
