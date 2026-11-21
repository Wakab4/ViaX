package moe.pure.browser.ui.components.insets

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.core.view.WindowInsetsCompat
import moe.pure.browser.R

/** l3 keeps the requested mode while transient system bars are visible. */
internal object NativeFullscreenState {
    fun snapshot(window: Window): Int? = if (Build.VERSION.SDK_INT >= 30)
        window.decorView.getTag(R.id.page_fullscreen_request) as? Int else null

    @Suppress("DEPRECATION")
    fun requested(window: Window): Boolean {
        val decor = window.decorView
        if (Build.VERSION.SDK_INT >= 30) {
            snapshot(window)?.let { return it == 1 }
            decor.rootWindowInsets?.let { return !it.isVisible(WindowInsets.Type.statusBars()) }
        }
        return decor.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
    }

    fun set(window: Window, enabled: Boolean) = restore(window, if (enabled) 1 else 0)
    @Suppress("DEPRECATION")
    fun apply(window: Window, enabled: Boolean) {
        set(window, enabled)
        val decor = window.decorView
        if (Build.VERSION.SDK_INT >= 30) {
            if (Build.VERSION.SDK_INT < 35) window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                if (enabled) {
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsets.Type.systemBars())
                } else {
                    if (Build.VERSION.SDK_INT >= 31) controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_DEFAULT
                    controller.show(WindowInsets.Type.systemBars())
                }
                decor.requestApplyInsets()
                return
            }
        }
        window.setFlags(if (enabled) WindowManager.LayoutParams.FLAG_FULLSCREEN else 0,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val mask = if (enabled || Build.VERSION.SDK_INT < 29) LEGACY_FULLSCREEN else LEGACY_HIDE_BARS
        decor.systemUiVisibility = if (enabled) decor.systemUiVisibility or mask else decor.systemUiVisibility and mask.inv()
        decor.requestApplyInsets()
    }
    fun restore(window: Window, value: Int?) {
        if (Build.VERSION.SDK_INT >= 30) window.decorView.setTag(R.id.page_fullscreen_request, value)
    }

    @Suppress("DEPRECATION")
    fun requested(view: View, insets: WindowInsetsCompat): Boolean {
        if (Build.VERSION.SDK_INT >= 30) {
            val value = view.rootView.getTag(R.id.page_fullscreen_request)
            if (value is Int) return value == 1
            return !insets.isVisible(WindowInsetsCompat.Type.statusBars())
        }
        return view.rootView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN != 0
    }
    private const val LEGACY_HIDE_BARS = View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    private const val LEGACY_FULLSCREEN = LEGACY_HIDE_BARS or
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
}
