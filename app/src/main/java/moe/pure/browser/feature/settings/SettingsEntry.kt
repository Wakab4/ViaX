package moe.pure.browser.feature.settings

import android.app.Activity
import moe.pure.browser.feature.settings.host.SettingsRuntimeFragment
import moe.pure.browser.feature.settings.navigation.SettingsNavigation
import moe.pure.browser.ui.theme.ThemedActivity

/** Settings is a record in the current window, retaining the browser and its live pages beneath it. */
object SettingsEntry {
    fun open(activity: Activity, route: SettingsRoute = SettingsRoute.ROOT) {
        val window = activity as ThemedActivity
        if (window.supportFragmentManager.isStateSaved) return
        val runtime = SettingsRuntimeFragment.ensure(window)
        if (route == SettingsRoute.CUSTOMIZE) runtime.customize()
        else SettingsNavigation.enter(window, route)
    }
}
