package moe.via.browser.browser.ui.menu.customize

import android.app.Activity
import moe.via.browser.ui.theme.ThemedActivity

object MenuCustomizationEntry {
    fun open(activity: Activity) { (activity as ThemedActivity).pageStack.push(MenuCustomizationFragment::class.java, null) }
}
