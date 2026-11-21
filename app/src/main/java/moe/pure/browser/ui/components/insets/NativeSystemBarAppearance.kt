package moe.pure.browser.ui.components.insets

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController

/** Change icon contrast without changing system-bar visibility. */
internal object NativeSystemBarAppearance {
    @Suppress("DEPRECATION")
    fun light(window: Window, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= 30) {
            val mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (enabled) mask else 0, mask)
            return
        }
        val flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        val decor = window.decorView
        decor.systemUiVisibility = (decor.systemUiVisibility and flags.inv()) or if (enabled) flags else 0
    }
}
