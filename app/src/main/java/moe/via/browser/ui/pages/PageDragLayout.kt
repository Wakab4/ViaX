package moe.via.browser.ui.pages

import android.content.Context
import android.graphics.Canvas
import android.util.SparseBooleanArray
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.customview.widget.ViewDragHelper
import kotlin.math.abs
import kotlin.math.roundToInt
import moe.via.browser.ui.components.dp

/** Native widget.y: full-width capture, directional settling and the exposed-page dim. */
internal class PageDragLayout(context: Context, private val listener: Listener) : FrameLayout(context), AutoCloseable {
    interface Listener {
        fun begin(direction: Int): Boolean
        fun progress(fraction: Float)
        fun cancelled()
        fun completed()
    }
    private var page: View? = null
    private var enabled = true
    private var predictive = false
    val predictiveActive get() = predictive
    private var direction = if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) -1 else 1
    private var progress = 0f
    private var captured = false
    private var finished = false
    private val eligible = SparseBooleanArray()
    private val threshold get() = minOf(dp(200).toFloat() / resources.displayMetrics.widthPixels.coerceAtLeast(1), .46f)
    private val drag: ViewDragHelper = ViewDragHelper.create(this, .18f, object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            if (child !== page || child.translationX != 0f || !eligible.get(pointerId)) return false
            if (!captured) { captured = listener.begin(direction); finished = false }
            return captured
        }
        override fun getViewHorizontalDragRange(child: View) = 1
        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int) =
            if (direction > 0) left.coerceIn(0, child.width) else left.coerceIn(-child.width, 0)
        override fun clampViewPositionVertical(child: View, top: Int, dy: Int) = child.top
        override fun onViewPositionChanged(child: View, left: Int, top: Int, dx: Int, dy: Int) {
            // Fallback DEX has int-to-float before division; normal JADX output loses that cast.
            progress = abs(left.toFloat() / width.coerceAtLeast(1))
            listener.progress(progress)
            if (progress >= 1f && !finished) { finished = true; listener.completed() }
            invalidate()
        }
        override fun onViewReleased(child: View, xvel: Float, yvel: Float) {
            val velocity = if (direction > 0) xvel > 300f && xvel > abs(yvel) else xvel < -300f && xvel < -abs(yvel)
            val dismiss = progress > threshold || velocity && progress > threshold * 2f / 5f
            if (drag.settleCapturedViewAt(if (dismiss) direction * (child.width + 10) else 0, child.top)) postInvalidateOnAnimation()
        }
        override fun onViewDragStateChanged(state: Int) {
            if (state == ViewDragHelper.STATE_IDLE) {
                eligible.clear()
                if (captured && !finished) listener.cancelled()
                captured = false
            }
        }
    })

    fun content(view: View) { page = view; addView(view, LayoutParams(-1, -1)) }
    fun gestures(enabled: Boolean) { this.enabled = enabled }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            eligible.clear(); direction = if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) -1 else 1
        }
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            val index = event.actionIndex
            // g6.y.a0 sets edgeSize to the complete display width on either physical edge.
            eligible.put(event.getPointerId(index), event.getX(index) >= 0f && event.getX(index) <= width)
        }
        return super.dispatchTouchEvent(event)
    }
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean =
        if (predictive || !enabled && drag.viewDragState == ViewDragHelper.STATE_IDLE) super.onInterceptTouchEvent(event)
        else drag.shouldInterceptTouchEvent(event)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (predictive || !enabled && drag.viewDragState == ViewDragHelper.STATE_IDLE) return super.onTouchEvent(event)
        drag.processTouchEvent(event); return true
    }
    override fun computeScroll() { if (drag.continueSettling(true)) postInvalidateOnAnimation() }
    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        val result = super.drawChild(canvas, child, drawingTime)
        if (child === page && (predictive || drag.viewDragState != ViewDragHelper.STATE_IDLE)) {
            val amount = (64 * (1f - progress).coerceIn(0f, 1f)).toInt()
            if (amount > 0) {
                val checkpoint = canvas.save()
                if (direction > 0) canvas.clipRect(0f, 0f, child.left + child.translationX, height.toFloat())
                else canvas.clipRect(child.right + child.translationX, 0f, width.toFloat(), height.toFloat())
                canvas.drawColor(amount shl 24); canvas.restoreToCount(checkpoint)
            }
        }
        return result
    }
    fun beginPredictive(): Boolean {
        val child = page ?: return false
        if (predictive || child.translationX != 0f || drag.viewDragState != ViewDragHelper.STATE_IDLE) return false
        // Native predictive starts from the left even when manual dragging follows RTL.
        direction = 1
        if (!listener.begin(direction)) return false
        captured = true; finished = false; predictive = true; predictiveProgress(0f); return true
    }
    fun predictiveProgress(value: Float) {
        if (!predictive) return
        progress = value.coerceIn(0f, .999f)
        page?.translationX = (page?.width ?: 0) * progress
        listener.progress(progress); invalidate()
    }
    fun settlePredictive(complete: Boolean) {
        if (!predictive) return
        val child = page ?: return
        val left = child.left + child.translationX.roundToInt()
        child.translationX = 0f; child.offsetLeftAndRight(left - child.left); predictive = false
        if (drag.smoothSlideViewTo(child, if (complete) child.width + 10 else 0, child.top)) postInvalidateOnAnimation()
        else if (complete) { finished = true; listener.completed() } else { captured = false; listener.cancelled() }
    }
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (oldWidth == 0 || width == oldWidth && height == oldHeight) return
        val active = captured || predictive
        finished = true; captured = false; predictive = false; drag.abort(); progress = 0f
        page?.apply { translationX = 0f; offsetLeftAndRight(-left) }
        if (active) listener.cancelled()
    }
    override fun close() {
        finished = true; captured = false; predictive = false; drag.abort(); eligible.clear()
        page?.apply { animate().cancel(); translationX = 0f; offsetLeftAndRight(-left) }
    }
    companion object {
        fun gestures(view: View, enabled: Boolean) {
            var parent = view.parent
            while (parent != null) { if (parent is PageDragLayout) { parent.gestures(enabled); return }; parent = parent.parent }
        }
    }
}
