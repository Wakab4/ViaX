package moe.pure.browser.feature.gestures

import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.webkit.WebView
import moe.pure.browser.feature.settings.data.NativeAction
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.min

/** GP z8.b0.V: 80% viewport steps, preserving ongoing scroll animations. */
object NativePageScroller {
    private val animations = WeakHashMap<WebView, ObjectAnimator>()

    fun scroll(view: WebView?, action: NativeAction): Boolean {
        if (action !in setOf(NativeAction.TOP, NativeAction.BOTTOM, NativeAction.PAGE_UP, NativeAction.PAGE_DOWN)) return false
        if (view == null || animations[view]?.isRunning == true) return true
        val start = view.scrollY
        val height = view.measuredHeight
        val content = view.contentHeight * view.resources.displayMetrics.density
        val delta = when (action) {
            NativeAction.TOP -> -start
            NativeAction.BOTTOM -> content.toInt() - start
            NativeAction.PAGE_UP -> -min(height / 5 * 4, start)
            else -> min(height / 5 * 4, (content - height - start).toInt())
        }
        if (delta == 0) return true
        animations[view] = ObjectAnimator.ofInt(view, "scrollY", start, start + delta).apply {
            duration = (min(1f, abs(delta) / 3000f) * 400f + 100f).toLong()
            interpolator = AccelerateDecelerateInterpolator()
            setAutoCancel(true)
            start()
        }
        return true
    }

    fun cancel(view: WebView) { animations.remove(view)?.cancel() }
}
