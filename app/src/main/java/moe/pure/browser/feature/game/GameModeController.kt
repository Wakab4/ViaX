package moe.pure.browser.feature.game

import moe.pure.browser.ui.message.BrowserMessages

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.ui.components.insets.NativeFullscreenState

/**
 * GP game mode protects page interaction and expands the WebView viewport.
 * It does not change JavaScript, WebSettings, orientation, or screen-awake policy.
 */
class GameModeController(
    private val activity: Activity,
    private val setChromeGame: (Boolean) -> Unit,
    private val preferredFullscreen: () -> Boolean = { BrowserSettings(activity).fullscreen },
    private val message: (String) -> Unit = { BrowserMessages.show(activity, it) }
) {
    private val handler = Handler(Looper.getMainLooper())
    private var previousBack: Long? = null
    private var disposed = false
    private val refreshInsets = Runnable {
        if (!disposed) activity.window.decorView.requestApplyInsets()
    }

    val active: Boolean get() = Companion.active

    /** Gate Via-style context menus, not WebView's own text selection or page touch events. */
    val allowBrowserLongPress: Boolean get() = !active
    val allowVolumePageActions: Boolean get() = !active
    val allowToolbarReveal: Boolean get() = !active
    val allowVideoGestures: Boolean get() = !active
    val allowVideoToolbar: Boolean get() = !active

    /** The caller applies the menu's internal-page availability rule before dispatch. */
    fun toggle(): Boolean {
        checkMain()
        if (!disposed) change(!active, announce = true)
        return active
    }

    /** Call after normal window preferences have been applied on resume. */
    fun onResume() {
        checkMain()
        if (!disposed && active) {
            setChromeGame(true)
            fullscreen(true)
        }
    }

    /** A newly constructed chrome tree must receive the current process-only state. */
    fun rebind() {
        checkMain()
        if (disposed) return
        setChromeGame(active)
        if (active) fullscreen(true)
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        checkMain()
        if (!disposed && hasFocus && active) fullscreen(true)
    }

    /** Call after dismissing browser overlays and leaving an HTML custom view. */
    fun onBack(): Boolean {
        checkMain()
        if (disposed || !active) return false
        val now = SystemClock.elapsedRealtime()
        val previous = previousBack
        if (previous == null || now - previous > BACK_INTERVAL_MS) {
            previousBack = now
            message("再按一次返回键退出游戏模式")
        } else {
            previousBack = null
            change(false, announce = true)
        }
        return true
    }

    private fun change(value: Boolean, announce: Boolean) {
        if (value == active) return
        processActive = value
        previousBack = null
        handler.removeCallbacks(refreshInsets)
        setChromeGame(value)
        fullscreen(value || preferredFullscreen())
        // Chrome owns the 180 ms motion and restores its measured viewport at 200 ms.
        // Reapply the window insets at that same boundary after restoring normal system bars.
        if (!value) handler.postDelayed(refreshInsets, 200L)
        if (announce) message(if (value) "游戏模式已开启，按两次返回键可退出" else "游戏模式 已关闭")
    }

    private fun fullscreen(enabled: Boolean) = NativeFullscreenState.apply(activity.window, enabled)

    /** Retention is memory only; do not serialize active into saved state or preferences. */
    fun dispose(retain: Boolean) {
        checkMain()
        if (disposed) return
        handler.removeCallbacks(refreshInsets)
        previousBack = null
        if (!retain && active) change(false, announce = false)
        handler.removeCallbacks(refreshInsets)
        disposed = true
    }

    private fun checkMain() = check(Looper.myLooper() == Looper.getMainLooper()) { "Game mode belongs to the browser main thread" }

    companion object {
        @Volatile private var processActive = false
        val active: Boolean get() = processActive
        const val BACK_INTERVAL_MS = 1500L
    }
}
