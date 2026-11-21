package moe.pure.browser.ui.components

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import moe.pure.browser.ui.theme.palette

/** Popovers cover the page; system and browser toolbars keep their own appearance. */
class ContentOverlay(private val activity: Activity, private val parent: FrameLayout, private val closed: () -> Unit = {}) {
    private var layer: FrameLayout? = null
    val isShowing get() = layer != null

    fun show(body: View, width: Int, top: Boolean, end: Boolean = false, fullHeight: Boolean = false) {
        close()
        val overlay = FrameLayout(activity).apply {
            setBackgroundColor(0x38000000)
            contentDescription = "隐藏弹出式窗口"
            setOnClickListener { close() }
        }
        body.background = GradientDrawable().apply {
            setColor(if (fullHeight) activity.palette.canvas else if (activity.palette.night) 0xCC000000.toInt() else 0xEBFFFFFF.toInt())
            val radius = if (fullHeight) 0f else activity.dp(18).toFloat()
            cornerRadii = if (top) floatArrayOf(0f,0f,0f,0f,radius,radius,radius,radius)
                else floatArrayOf(radius,radius,radius,radius,0f,0f,0f,0f)
        }
        body.isClickable = true
        body.clipToOutline = true
        overlay.addView(body, FrameLayout.LayoutParams(width, if (fullHeight) -1 else -2,
            (if (top) Gravity.TOP else Gravity.BOTTOM) or if (end) Gravity.END else Gravity.START))
        parent.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        layer = overlay
        overlay.alpha = 0f
        overlay.translationY = if (top) -120f else 120f
        overlay.animate().alpha(1f).translationY(0f).setDuration(160)
            .setInterpolator(android.view.animation.AccelerateInterpolator()).start()
    }
    fun close(): Boolean {
        val existing = layer ?: return false
        parent.removeView(existing); layer = null
        closed()
        return true
    }
}
