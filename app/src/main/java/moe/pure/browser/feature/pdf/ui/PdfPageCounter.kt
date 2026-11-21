package moe.pure.browser.feature.pdf.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.widget.TextView
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.surface
import moe.pure.browser.ui.theme.palette
import moe.pure.browser.ui.pages.PageDragLayout
import kotlin.math.abs

/** za.g's page count is visible on movement, then fades 2 seconds after touch-up. */
internal class PdfPageCounter(context: Context, private val canvas: PdfCanvasView) : AutoCloseable {
    val view = NativeTypography.text(context).apply {
        textSize = 12f; setTextColor(context.palette.secondary)
        background = context.surface(context.palette.canvas, 18)
        setPadding(dp(8), dp(4), dp(8), dp(4))
        alpha = 0f; visibility = View.INVISIBLE
        layoutParams = FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply { leftMargin = context.dp(12); topMargin = context.dp(12) }
    }
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var decided = false
    private val hide = Runnable {
        view.animate().alpha(0f).setDuration(220).withEndAction { if (view.alpha == 0f) view.visibility = View.INVISIBLE }.start()
    }
    fun page(index: Int, total: Int) { view.text = "${index + 1} / $total" }
    fun touch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y; decided = false
                PageDragLayout.gestures(canvas, false)
                if (canvas.zoom > 1f) canvas.parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> {
                view.removeCallbacks(hide); view.animate().cancel(); view.visibility = View.VISIBLE
                view.animate().alpha(1f).setDuration(120).start()
                if (canvas.zoom > 1f || event.pointerCount > 1) {
                    decided = true; PageDragLayout.gestures(canvas, false); canvas.parent?.requestDisallowInterceptTouchEvent(true)
                }
                else if (decided || maxOf(abs(event.x - downX), abs(event.y - downY)) >= slop) {
                    decided = true
                    val horizontal = abs(event.x - downX) > abs(event.y - downY)
                    PageDragLayout.gestures(canvas, horizontal)
                    canvas.parent?.requestDisallowInterceptTouchEvent(!horizontal)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> { decided = true; PageDragLayout.gestures(canvas, false); canvas.parent?.requestDisallowInterceptTouchEvent(true) }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                decided = false; PageDragLayout.gestures(canvas, true); view.removeCallbacks(hide); view.postDelayed(hide, 2000)
            }
        }
    }
    fun refreshTheme() { view.setTextColor(view.context.palette.secondary); view.background = view.context.surface(view.context.palette.canvas, 18) }
    override fun close() { PageDragLayout.gestures(canvas, true); view.removeCallbacks(hide); view.animate().cancel() }
}
