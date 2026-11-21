package moe.pure.browser.feature.home.ui.customize

import moe.pure.browser.ui.theme.ThemedActivity

internal object HomeCustomizeEntry {
    fun open(activity: ThemedActivity) { activity.currentRecordStack?.push(HomeCustomizeFragment::class.java, null) }
}
