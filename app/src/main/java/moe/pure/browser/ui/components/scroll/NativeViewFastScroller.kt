package moe.pure.browser.ui.components.scroll

import android.graphics.Canvas
import android.graphics.Interpolator
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.StateListDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AnimationUtils
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette
import kotlin.math.abs

/** q4.a + r3.h/i: the original thumb geometry, scrolling conversion, tap forwarding and fade engine. */
internal class NativeViewFastScroller(private val view: View, private val surface: Surface, private val pdfStyle: Boolean = false) {
    interface Surface {
        fun scrollRange(): Int
        fun scrollExtent(): Int
        fun scrollOffset(): Int
        fun ordinaryTouch(event: MotionEvent): Boolean
    }
    private val rtl = !pdfStyle && view.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    private val bounds = Rect(0, 0, view.dp(if (pdfStyle) 20 else 24), view.dp(if (pdfStyle) 32 else 64))
    private val thumb = StateListDrawable().apply {
        if (pdfStyle) {
            fun nativeShape(color: Int) = InsetDrawable(GradientDrawable().apply {
                setColor(color); cornerRadius = view.dp(20) / 2f
            }, view.dp(12), view.dp(4), view.dp(4), view.dp(4))
            addState(intArrayOf(android.R.attr.state_pressed), nativeShape(0xff03a9f4.toInt()))
            addState(intArrayOf(), nativeShape(0x80808080.toInt()))
        } else {
        fun shape(color: Int, inset: Int) = LayerDrawable(arrayOf(GradientDrawable().apply {
            setColor(color)
            val radius = view.dp(999).toFloat()
            cornerRadii = if (rtl) floatArrayOf(0f, 0f, radius, radius, radius, radius, 0f, 0f)
                else floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
        })).apply { setLayerInset(0, if (rtl) 0 else inset, 0, if (rtl) inset else 0, 0) }
        addState(intArrayOf(android.R.attr.state_pressed), shape(view.context.palette.accent, view.dp(18)))
        addState(intArrayOf(), shape(0x80808080.toInt(), view.dp(19)))
        }
    }
    private val fade = Interpolator(1, 2)
    private val fadeValues = FloatArray(1)
    private var fadeState = 0
    private var fadeAt = 0L
    private var dragging = false
    private var moved = false
    private var lastY = 0f
    private val slop = view.dp(4)
    private val fadeOut = Runnable {
        val now = AnimationUtils.currentAnimationTimeMillis()
        if (now >= fadeAt) {
            fade.setKeyFrame(0, now.toInt(), floatArrayOf(255f))
            fade.setKeyFrame(1, now.toInt() + ViewConfiguration.getScrollBarFadeDuration(), floatArrayOf(0f))
            fadeState = 2
            view.invalidate()
        }
    }
    fun awaken(): Boolean {
        view.postInvalidateOnAnimation()
        if (dragging) return false
        val delay = if (fadeState == 0) maxOf(750L, 1500L) else 1500L
        fadeAt = AnimationUtils.currentAnimationTimeMillis() + delay
        fadeState = 1
        view.removeCallbacks(fadeOut); view.postDelayed(fadeOut, delay)
        return false
    }
    fun draw(canvas: Canvas) {
        if (!dragging && fadeState == 0) return
        val fading = !dragging && fadeState == 2
        if (fading) {
            if (fade.timeToValues(fadeValues) == Interpolator.Result.FREEZE_END) fadeState = 0
            else thumb.alpha = Math.round(fadeValues[0])
        } else thumb.alpha = 255
        if (position(0)) {
            thumb.setBounds(bounds.left + view.scrollX, bounds.top + view.scrollY, bounds.right + view.scrollX, bounds.bottom + view.scrollY)
            thumb.draw(canvas)
        }
        // Native invalidates once more at FREEZE_END, so the final faint thumb is cleared.
        if (fading) view.invalidate()
    }
    fun intercept(event: MotionEvent) = event.actionMasked == MotionEvent.ACTION_DOWN && touch(event)
    fun touch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (fadeState == 0) { dragging = false; return false }
                if (!dragging) {
                    moved = false
                    position(0)
                    if (event.y >= bounds.top && event.y <= bounds.bottom && event.x >= bounds.left && event.x <= bounds.right) {
                        dragging = true; lastY = event.y
                        surface.ordinaryTouch(event)
                        MotionEvent.obtain(event).also { it.action = MotionEvent.ACTION_CANCEL; surface.ordinaryTouch(it); it.recycle() }
                        pressed(true); view.removeCallbacks(fadeOut)
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> if (dragging) {
                val delta = Math.round(event.y - lastY)
                if (delta != 0) {
                    if (moved) { position(delta); lastY = event.y }
                    else if (abs(delta) > slop) { position(if (delta > 0) delta - slop else delta + slop); moved = true; lastY = event.y }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (dragging) {
                pressed(false); dragging = false; awaken()
                if (event.actionMasked == MotionEvent.ACTION_UP && !moved) {
                    MotionEvent.obtain(event).also { it.action = MotionEvent.ACTION_DOWN; surface.ordinaryTouch(it); it.recycle() }
                    surface.ordinaryTouch(event)
                }
            }
        }
        if (dragging) { view.invalidate(); view.parent?.requestDisallowInterceptTouchEvent(true) }
        return dragging
    }
    private fun pressed(value: Boolean) {
        thumb.state = if (value) intArrayOf(android.R.attr.state_pressed) else intArrayOf()
        view.invalidate()
    }
    private fun position(delta: Int): Boolean {
        val range = surface.scrollRange()
        if (range <= 0) return false
        val offset = surface.scrollOffset()
        val extent = surface.scrollExtent()
        val remaining = range - extent
        val height = view.height
        if (remaining <= height) return false
        val width = bounds.width()
        bounds.left = if (rtl) 0 else view.width - width; bounds.right = bounds.left + width
        val thumbHeight = maxOf(view.dp(if (pdfStyle) 32 else 64), Math.round(minOf(extent.toFloat() / range, .25f) * height))
        val travel = height - thumbHeight
        val top = Math.round(offset.toFloat() / remaining * travel)
        bounds.top = top; bounds.bottom = top + thumbHeight
        if (delta != 0) {
            val proposed = top + delta
            val target = if (proposed <= travel) { if (proposed < 0) 0 else proposed } else travel
            view.scrollBy(0, Math.round(remaining * (target.toFloat() / travel)) - offset)
        }
        return true
    }
    fun cancel() { dragging = false; view.removeCallbacks(fadeOut); fadeState = 0; pressed(false) }
}

