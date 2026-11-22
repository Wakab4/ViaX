package moe.via.browser.feature.ai.ui.settings

import moe.via.browser.ui.theme.ThemedActivity

internal object AiSettingsEntry {
    fun open(activity: ThemedActivity) { activity.pageStack.push(AiSettingsFragment::class.java, null) }
}
