package moe.pure.browser.feature.gestures

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import moe.pure.browser.feature.settings.data.ActionBinding
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.feature.settings.data.NativeAction
import java.lang.ref.WeakReference
import kotlin.math.abs

/** Input consumer only. The browser owns navigation, panels, settings and View listener composition. */
class BrowserInputController(
    private val context: Context,
    private val preferences: BrowserPreferences,
    private val callbacks: BrowserInputCallbacks,
) {
    private var disposed = false
    private var pageOverscrollEligible = false
    private var toolbarPreview = 0
    private val toolbars = mutableListOf<WeakReference<ToolbarGestureLayout>>()
    private val keys = BrowserHardwareKeys(preferences, callbacks)
    private val swipe = PageSwipeGesture(context, ::swipeAllowed, callbacks::state, { backward ->
        callbacks.execute(if (backward) NativeAction.BACK else NativeAction.FORWARD)
    }, ::verticalSwipe, callbacks::onPageGesturePreview)

    /** Add above the browser chrome's content using its supplied FrameLayout.LayoutParams. */
    val pageIndicator: View get() = swipe.indicator

    /** Compose with the page's existing listener; Native returns false for every page motion event. */
    fun onTouch(view: View, event: MotionEvent): Boolean {
        if (disposed) return false
        val state = callbacks.state()
        if (!state.browserVisible || state.contentOverlayOpen || state.inputEditing || state.customViewShowing) return false
        swipe.onTouch(view, event)
        return false
    }

    /** Call from WebView.onTouchEvent(DOWN), after listener dispatch, as in GP t4.b. */
    fun onPageTouchDown(hasUrl: Boolean) { pageOverscrollEligible = !hasUrl }

    /** Native l() becomes true after horizontal WebView overscroll, and lasts until the next DOWN. */
    fun onPageOverScrolled(clampedX: Boolean) { if (clampedX) pageOverscrollEligible = true }

    fun onKey(event: KeyEvent): Boolean = !disposed && keys.onKey(event)

    /** Five button bindings consume long press even when their action is NONE. */
    fun onLongPress(binding: ActionBinding): Boolean {
        if (disposed || !callbacks.state().browserVisible || binding == ActionBinding.TOOLBAR_LEFT || binding == ActionBinding.TOOLBAR_RIGHT) return false
        callbacks.execute(preferences.action(binding))
        return true
    }

    fun createToolbarLayout(): ToolbarGestureLayout = ToolbarGestureLayout(context, {
        val state = callbacks.state()
        !disposed && state.browserVisible && !state.gameMode && !state.customViewShowing && !state.inputEditing
    }, ::toolbarDrag).also { toolbars.add(WeakReference(it)) }

    fun reset() {
        pageOverscrollEligible = false
        swipe.reset()
        toolbars.removeAll { it.get() == null }
        toolbars.forEach { it.get()?.reset() }
    }

    fun dispose() {
        if (disposed) return
        reset(); disposed = true; toolbars.clear()
    }

    private fun swipeAllowed(): Boolean {
        val state = callbacks.state()
        return preferences.swipeNavigation && pageOverscrollEligible &&
            !(state.customTab && state.gameMode && !state.chromeVisible)
    }

    private fun verticalSwipe(downward: Boolean) {
        val state = callbacks.state()
        if (state.customTab || state.inputEditing || state.keyboardVisible || (!state.gameMode && !preferences.fullscreen && preferences.autoHide == 0)) return
        if (downward && (state.gameMode || preferences.autoHide == 2)) return
        callbacks.setChromeVisible(downward)
    }

    private fun toolbarDrag(fraction: Float, released: Boolean) {
        if (disposed) return
        val armed = abs(fraction.toDouble()) > .8
        val binding = if (fraction > 0f) ActionBinding.TOOLBAR_LEFT else ActionBinding.TOOLBAR_RIGHT
        val action = preferences.action(binding)
        if (released && armed) callbacks.execute(action)
        val preview = if (!armed) 0 else when (action) {
            NativeAction.PREVIOUS_TAB -> -1
            NativeAction.NEXT_TAB -> 1
            else -> 0
        }
        if (toolbarPreview != preview) { toolbarPreview = preview; callbacks.onToolbarPreview(preview) }
    }
}
