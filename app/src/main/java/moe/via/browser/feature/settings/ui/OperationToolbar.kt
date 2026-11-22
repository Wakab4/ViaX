package moe.via.browser.feature.settings.ui

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.abs

/** The reference draggable host caps translation at 300 physical pixels and commits at 80%. */
internal class OperationToolbar(context: Context, private val bar: View, private val selected: (Float) -> Unit) : FrameLayout(context) {
    private var downX = 0f
    private var downY = 0f
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    init { addView(bar, LayoutParams(-1, -2)); clipChildren = false }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) parent?.requestDisallowInterceptTouchEvent(true)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) parent?.requestDisallowInterceptTouchEvent(false)
        return super.dispatchTouchEvent(event)
    }
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean = when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; bar.animate().cancel(); false }
        MotionEvent.ACTION_MOVE -> abs(event.x - downX) > slop && abs(event.x - downX) > abs(event.y - downY)
        else -> false
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> bar.translationX = (event.x - downX).coerceIn(-300f, 300f)
            MotionEvent.ACTION_UP -> {
                val distance = bar.translationX / 300f
                bar.animate().translationX(0f).setDuration(180).start()
                if (abs(distance) >= .8f) selected(if (layoutDirection == LAYOUT_DIRECTION_RTL) -distance else distance)
            }
            MotionEvent.ACTION_CANCEL -> bar.animate().translationX(0f).setDuration(180).start()
        }
        return true
    }
}
