package moe.pure.browser.feature.menu.customize

import android.app.Activity
import moe.pure.browser.ui.theme.ThemedActivity

object MenuCustomizationEntry {
    fun open(activity: Activity) { (activity as ThemedActivity).pageStack.push(MenuCustomizationFragment::class.java, null) }
}
