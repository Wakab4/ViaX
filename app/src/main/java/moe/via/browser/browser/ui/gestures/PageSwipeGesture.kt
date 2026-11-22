package moe.via.browser.browser.ui.gestures

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import kotlin.math.abs
import kotlin.math.max

/** GP mark.via.common.widget.v0, with the arrow view rather than the webpage as the drag target. */
internal class PageSwipeGesture(
    private val context: Context,
    private val allowed: () -> Boolean,
    private val state: () -> BrowserInputState,
    private val onNavigate: (Boolean) -> Unit,
    private val onVertical: (Boolean) -> Unit,
    private val onPreview: (PageGesturePreview) -> Unit,
) {
    val indicator = ImageView(context).apply {
        layoutParams = FrameLayout.LayoutParams(dp(72), dp(60), Gravity.START or Gravity.CENTER_VERTICAL).apply {
            marginStart = -dp(73)
        }
        setPadding(dp(20), dp(20), dp(20), dp(20))
        imageTintList = ColorStateList.valueOf(Color.WHITE)
        elevation = dp(12).toFloat()
        background = GradientDrawable().apply {
            setColor(0xE64E4E4E.toInt()); cornerRadius = dp(18).toFloat()
            setStroke(1, 0x40808080)
        }
        visibility = View.INVISIBLE
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private var width = 0
    private var lastX = 0
    private var lastY = 0
    private var downY = 0
    private var accumulatedX = 0
    private var arrowPosition = 0
    private var arrowOrigin = 0
    private var mode = -1
    private var active = false
    private var signal = 12
    private var lastTime = 0L
    private var animation: ObjectAnimator? = null
    private var direction = 0

    fun onTouch(view: View, event: MotionEvent) {
        val threshold = ((indicator.width.takeIf { it > 0 } ?: context.dp(72)) * 4 / 5).coerceAtLeast(1)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                width = view.width
                val current = state()
                val canNavigate = !view.canScrollHorizontally(1) && !view.canScrollHorizontally(-1)
                signal = 12 or (if (canNavigate && current.backAvailable) 1 else 0) or
                    (if (canNavigate && current.forwardAvailable) 2 else 0)
                active = false
                lastX = event.rawX.toInt(); lastY = event.rawY.toInt(); downY = lastY
                accumulatedX = 0; mode = -1; direction = 0
                animation?.cancel(); animation = null
                indicator.alpha = 1f; indicator.translationX = 0f; indicator.visibility = View.VISIBLE
                lastTime = SystemClock.elapsedRealtime()
            }
            MotionEvent.ACTION_MOVE -> {
                if (!active) {
                    active = allowed(); accumulatedX = 0
                    return
                }
                var dx = (event.rawX - lastX).toInt()
                val dy = (event.rawY - lastY).toInt()
                lastX = event.rawX.toInt(); lastY = event.rawY.toInt()
                if (mode == -1) {
                    val time = SystemClock.elapsedRealtime()
                    val elapsed = max(1L, time - lastTime).toFloat()
                    val vx = dx / elapsed; val vy = dy / elapsed
                    lastTime = time
                    if (abs(vx) <= abs(vy) || abs(vx) <= .5f || abs(dx) <= abs(dy) + context.dp(4)) {
                        mode = 0
                    } else {
                        val bit = if (dx > 0) 1 else 2
                        if (signal and bit == bit) {
                            mode = 1; direction = bit
                            arrowOrigin = if (dx > 0) -10 else width + indicator.layoutParams.width + 10
                            arrowPosition = arrowOrigin
                            indicator.setImageResource(if (dx > 0) R.drawable.ref_ic_back else R.drawable.ref_ic_forward)
                            indicator.translationX = arrowPosition.toFloat()
                        } else mode = 0
                    }
                }
                if (mode == 1) dx = dx * 8 / 10
                if (accumulatedX + dx > threshold && dx > 0) dx = threshold - accumulatedX
                else if (accumulatedX + dx < -threshold && dx < 0) dx = -threshold - accumulatedX
                if ((accumulatedX > 0 && dx + accumulatedX <= 0) || (accumulatedX < 0 && dx + accumulatedX >= 0)) dx = 0
                if (mode == 1) {
                    arrowPosition += dx
                    indicator.translationX = arrowPosition.toFloat()
                    publish()
                }
                accumulatedX += dx
            }
            MotionEvent.ACTION_UP -> {
                active = false
                if (mode == 1) {
                    if (abs(accumulatedX) < threshold) {
                        animation = ObjectAnimator.ofFloat(indicator, "translationX", arrowPosition.toFloat(), arrowOrigin.toFloat()).apply {
                            duration = (abs(accumulatedX).toFloat() / threshold * 300f).toLong(); start()
                        }
                    } else {
                        animation = ObjectAnimator.ofFloat(indicator, "alpha", 1f, 0f).apply { duration = 300 }
                        if (accumulatedX >= threshold) onNavigate(true)
                        else if (accumulatedX <= -threshold) onNavigate(false)
                        animation?.start()
                    }
                } else {
                    val totalY = event.rawY.toInt() - downY
                    if (abs(totalY) > context.dp(12) * 3) onVertical(totalY > 0)
                }
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (mode == 1) indicator.translationX = 0f
                mode = 0
            }
            MotionEvent.ACTION_CANCEL -> reset()
        }
    }

    fun reset() {
        animation?.cancel(); animation = null
        active = false; mode = -1; accumulatedX = 0; direction = 0
        indicator.translationX = 0f; indicator.alpha = 1f
        publish()
    }

    private fun publish() = onPreview(PageGesturePreview(direction, indicator.translationX, indicator.alpha))
}
