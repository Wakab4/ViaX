package moe.pure.browser.feature.ai.ui.settings

import moe.pure.browser.ui.theme.ThemedActivity

internal object AiSettingsEntry {
    fun open(activity: ThemedActivity) { activity.pageStack.push(AiSettingsFragment::class.java, null) }
}
