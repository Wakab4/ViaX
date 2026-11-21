package moe.pure.browser.feature.sync.ui

import android.app.Activity
import moe.pure.browser.ui.theme.ThemedActivity

object SyncSettingsEntry {
    fun open(activity: Activity) { (activity as ThemedActivity).pageStack.push(SyncSettingsFragment::class.java, null) }
}
