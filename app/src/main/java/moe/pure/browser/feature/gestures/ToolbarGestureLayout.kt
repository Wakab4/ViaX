package moe.pure.browser.feature.gestures

import android.content.Context
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.customview.widget.ViewDragHelper
import moe.pure.browser.ui.components.dp

/** The GP com.tuyafeng.support.widget.a host. Place the whole toolbar inside this FrameLayout. */
class ToolbarGestureLayout(
    context: Context,
    private val allowed: () -> Boolean = { true },
    private val onDrag: (fraction: Float, released: Boolean) -> Unit,
) : FrameLayout(context) {
    var dragDistancePx: Int = context.dp(84)
        set(value) { require(value > 0); field = value }
    /** The settings preview uses the same host while blocking its vertical parent from intercepting. */
    var disallowParentIntercept: Boolean = false
    private var fraction = 0f
    private val rtl = TextUtils.getLayoutDirectionFromLocale(resources.configuration.locales[0]) == LAYOUT_DIRECTION_RTL
    private val drag: ViewDragHelper = ViewDragHelper.create(this, object : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View, pointerId: Int) = true
        override fun getViewHorizontalDragRange(child: View) = 1
        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int) =
            left.coerceIn(paddingLeft - dragDistancePx, paddingLeft + dragDistancePx)
        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            fraction = (left - paddingLeft).toFloat() / dragDistancePx
            onDrag(if (rtl) -fraction else fraction, false)
            invalidate()
        }
        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            onDrag(if (rtl) -fraction else fraction, true)
            drag.settleCapturedViewAt(paddingLeft, paddingTop)
            postInvalidateOnAnimation()
        }
    })

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (disallowParentIntercept) when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> parent?.requestDisallowInterceptTouchEvent(true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean =
        if (allowed()) drag.shouldInterceptTouchEvent(event) else super.onInterceptTouchEvent(event)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!allowed()) return super.onTouchEvent(event)
        drag.processTouchEvent(event)
        return true
    }

    override fun computeScroll() {
        super.computeScroll()
        if (drag.continueSettling(true)) postInvalidateOnAnimation()
    }

    fun reset() {
        drag.abort()
        fraction = 0f
        for (index in 0 until childCount) getChildAt(index).apply {
            offsetLeftAndRight(paddingLeft - left)
            offsetTopAndBottom(paddingTop - top)
        }
        onDrag(0f, false)
    }

    override fun onDetachedFromWindow() {
        drag.abort()
        super.onDetachedFromWindow()
    }
}
