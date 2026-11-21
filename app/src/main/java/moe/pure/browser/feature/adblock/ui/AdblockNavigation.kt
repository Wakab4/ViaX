package moe.pure.browser.feature.adblock.ui

import android.app.Activity
import android.os.Bundle
import moe.pure.browser.ui.theme.ThemedActivity

object AdblockNavigation {
    fun settings(activity: Activity) { (activity as ThemedActivity).pageStack.push(AdblockSettingsFragment::class.java, null) }
    fun editor(activity: Activity) { (activity as ThemedActivity).pageStack.push(FilterEditorFragment::class.java, null) }
    internal fun previewArguments(path: String?, title: String?) = Bundle().apply {
        putString("path", path); if (!title.isNullOrEmpty()) putString("title", title)
    }
}
