package moe.via.browser.browser.ui.chrome

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.SystemClock
import android.view.animation.PathInterpolator

/** Transient UI state for a chrome tree rebuilt after a configuration change. */
data class ChromeViewportState(val visible: Boolean, val restoreViewportAt: Long = 0L)

/** Coordinates Native 180 ms toolbar motion and the 200 ms game-exit viewport boundary. */
internal class ChromeVisibilityController(
    private val layout: ChromeViewportLayout,
    private val changed: () -> Unit
) {
    private var animator: ValueAnimator? = null
    private var generation = 0L
    private var disposed = false
    private var normalExpanded = false
    private var game = false
    private var keyboard = false
    private var targetVisible = true
    private var restoreAt = 0L
    val isVisible get() = targetVisible && !game
    private val settle = Runnable {
        if (!disposed) {
            restoreAt = 0L
            layout.setExpanded(game || normalExpanded)
            if (!game && !normalExpanded) {
                // ub() reinstates the ordinary bar constraints after game mode, even if IME blocked fb().
                targetVisible = true
                layout.moveTo(0f)
                layout.setBarsInLayout(true)
            }
            changed()
        }
    }

    fun policy(expanded: Boolean) {
        if (disposed) return
        normalExpanded = expanded
        if (restoreAt != 0L && restoreAt <= SystemClock.uptimeMillis()) restoreAt = 0L
        layout.setExpanded(game || expanded || restoreAt > SystemClock.uptimeMillis())
        if (!game && !expanded && restoreAt == 0L) show(true, false)
    }

    fun game(enabled: Boolean) {
        if (disposed || game == enabled) return
        game = enabled
        layout.removeCallbacks(settle)
        restoreAt = 0L
        layout.setExpanded(true)
        if (enabled) show(false, true)
        else {
            // fb() runs before ub(); the viewport stays expanded through the complete show animation.
            show(true, true)
            restoreAt = SystemClock.uptimeMillis() + 200L
            layout.postDelayed(settle, 200L)
        }
    }

    fun keyboard(visible: Boolean) {
        keyboard = visible
        layout.keyboard(visible)
    }

    fun show(visible: Boolean, animated: Boolean) {
        if (disposed || keyboard) return
        val shown = if (game) false else visible || !layout.expanded
        val destination = if (shown) 0f else 1f
        if (targetVisible == shown && animator != null && animated) return
        if (targetVisible == shown && animator == null && layout.hiddenFraction == destination) return
        targetVisible = shown
        val token = ++generation
        cancelAnimation()
        if (!animated || layout.width == 0 || layout.height == 0) {
            layout.moveTo(destination)
            layout.setBarsInLayout(shown)
            changed()
            return
        }
        val from = layout.hiddenFraction
        layout.setBarsInLayout(true)
        animator = ValueAnimator.ofFloat(from, destination).apply {
            duration = 180L
            interpolator = PathInterpolator(.2f, .2f, .8f, .8f)
            addUpdateListener { if (!disposed && token == generation) layout.moveTo(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (disposed || token != generation) return
                    animator = null
                    layout.moveTo(destination)
                    layout.setBarsInLayout(shown)
                    changed()
                }
            })
            start()
        }
        changed()
    }

    fun capture(visible: Boolean) = ChromeViewportState(visible, restoreAt)

    fun restore(state: ChromeViewportState) {
        if (disposed) return
        layout.removeCallbacks(settle)
        restoreAt = state.restoreViewportAt.takeIf { it > SystemClock.uptimeMillis() } ?: 0L
        layout.setExpanded(game || normalExpanded || restoreAt != 0L)
        if (restoreAt != 0L) layout.postDelayed(settle, (restoreAt - SystemClock.uptimeMillis()).coerceAtLeast(0L))
        show(state.visible, false)
    }

    fun dispose() {
        disposed = true
        generation++
        layout.removeCallbacks(settle)
        cancelAnimation()
    }

    private fun cancelAnimation() {
        animator?.removeAllListeners()
        animator?.cancel()
        animator = null
    }
}
