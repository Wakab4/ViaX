package moe.via.browser.feature.home.ui.customize

import moe.via.browser.ui.theme.ThemedActivity

internal object HomeCustomizeEntry {
    fun open(activity: ThemedActivity) { activity.currentRecordStack?.push(HomeCustomizeFragment::class.java, null) }
}
