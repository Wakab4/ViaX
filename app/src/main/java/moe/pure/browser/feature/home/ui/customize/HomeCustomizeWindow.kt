package moe.pure.browser.feature.home.ui.customize

import android.app.Activity
import android.os.Build
import android.view.View
import moe.pure.browser.ui.theme.palette

/** q.w3/v3 and l3.l/k retain the three original window values for this page's view. */
@Suppress("DEPRECATION")
internal class HomeCustomizeWindow(private val activity: Activity) : AutoCloseable {
    private val window = activity.window
    private val status = window.statusBarColor
    private val navigation = window.navigationBarColor
    private val appearance = if (Build.VERSION.SDK_INT >= 30) window.insetsController?.systemBarsAppearance ?: 0
        else window.decorView.systemUiVisibility
    fun apply() {
        val lightNavigation = !activity.palette.night
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.setSystemBarsAppearance(if (lightNavigation) 16 else 0, 24)
        } else {
            val bits = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            window.decorView.systemUiVisibility = (window.decorView.systemUiVisibility and bits.inv()) or
                if (lightNavigation) View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
        }
        if (Build.VERSION.SDK_INT < 29) {
            window.statusBarColor = CustomizeWorkspace.WORKSPACE
            window.navigationBarColor = if (activity.palette.night) 0xCC000000.toInt() else 0xEBFFFFFF.toInt()
            window.navigationBarDividerColor = window.navigationBarColor
        }
    }
    override fun close() {
        window.statusBarColor = status; window.navigationBarColor = navigation; window.navigationBarDividerColor = navigation
        if (Build.VERSION.SDK_INT >= 30) window.insetsController?.setSystemBarsAppearance(appearance, 24)
        else window.decorView.systemUiVisibility = appearance
    }
}
