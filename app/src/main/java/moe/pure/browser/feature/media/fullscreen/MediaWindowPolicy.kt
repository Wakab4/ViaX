package moe.pure.browser.feature.media.fullscreen

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Build
import android.view.Surface
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import moe.pure.browser.ui.components.insets.NativeFullscreenState

/** The browser's previous orientation and system bars are restored when the custom view ends. */
internal class MediaWindowPolicy(private val activity: Activity) {
    val previousOrientation = activity.requestedOrientation
    private val previousFlags = activity.window.attributes.flags
    private val previousFullscreenRequest = NativeFullscreenState.snapshot(activity.window)
    @Suppress("DEPRECATION") private val previousSystemUi = activity.window.decorView.systemUiVisibility
    private val previousBehavior = if (Build.VERSION.SDK_INT >= 30) activity.window.insetsController?.systemBarsBehavior else null
    private val previousStatusVisible = if (Build.VERSION.SDK_INT >= 30) activity.window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.statusBars()) else null
    private val previousNavigationVisible = if (Build.VERSION.SDK_INT >= 30) activity.window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.navigationBars()) else null

    val orientationAdjustable: Boolean = systemAllowsOrientation()

    private fun systemAllowsOrientation() = Build.VERSION.SDK_INT < 36 || activity.resources.configuration.smallestScreenWidthDp < 600

    @Suppress("DEPRECATION")
    fun immersive() {
        NativeFullscreenState.set(activity.window, true)
        if (Build.VERSION.SDK_INT >= 30) {
            if (Build.VERSION.SDK_INT < 35) activity.window.setDecorFitsSystemWindows(false)
            activity.window.insetsController?.apply {
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsets.Type.systemBars())
            }
        } else {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            activity.window.decorView.systemUiVisibility = activity.window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    fun orientation(mode: Int, landscapeVideo: Boolean, locked: Boolean) {
        if (!orientationAdjustable) return
        if (mode == 3) { request(previousOrientation); return }
        val landscape = when (mode) { 1 -> true; 2 -> false; else -> landscapeVideo }
        val requested = if (!locked) {
            if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            val rotation = if (Build.VERSION.SDK_INT >= 30) activity.display?.rotation else {
                @Suppress("DEPRECATION") activity.windowManager.defaultDisplay.rotation
            }
            val current = when (rotation) {
                Surface.ROTATION_0 -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                Surface.ROTATION_180 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                Surface.ROTATION_90 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                Surface.ROTATION_270 -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
            // These comparisons follow Native v.H, including its portrait reverse-rotation branch.
            if ((landscape && current != 0 && current != 8) || (!landscape && current != 1 && current != 8)) {
                if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else current
        }
        request(requested)
    }

    private fun request(orientation: Int) {
        if (systemAllowsOrientation() && activity.requestedOrientation != orientation) runCatching { activity.requestedOrientation = orientation }
    }

    @Suppress("DEPRECATION")
    fun restore() {
        request(previousOrientation)
        NativeFullscreenState.restore(activity.window, previousFullscreenRequest)
        activity.window.setFlags(previousFlags, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        activity.window.decorView.systemUiVisibility = previousSystemUi
        if (Build.VERSION.SDK_INT >= 30) activity.window.insetsController?.apply {
            previousBehavior?.let { systemBarsBehavior = it }
            previousStatusVisible?.let { if (it) show(WindowInsets.Type.statusBars()) else hide(WindowInsets.Type.statusBars()) }
            previousNavigationVisible?.let { if (it) show(WindowInsets.Type.navigationBars()) else hide(WindowInsets.Type.navigationBars()) }
        }
        activity.window.decorView.requestApplyInsets()
    }
}
