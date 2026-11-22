package moe.via.browser.browser.ui.gestures

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.SystemClock
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import kotlin.math.abs

/** GP fullscreenmode=2 floating control. Its host owns chrome/game/IME/custom-view visibility guards. */
class ToolbarRevealButton(private val activity: Activity, private val host: Host) {
    enum class Action { BACK, FORWARD, TABS, RELOAD }
    interface Host {
        fun revealToolbars()
        fun gesture(action: Action)
        /** Native fab: 80 center/bottom; 3 or 5 side/middle; 83 or 85 side/bottom. */
        fun position(): Int
        fun savePosition(position: Int)
    }

    private data class Snap(val x: Int, val y: Int, val position: Int)
    private var disposed = false
    private var visible = false
    private var touching = false
    private var longDrag = false
    private var axis = 0
    private var direction = 0
    private var rawX = 0
    private var rawY = 0
    private var downTime = 0L
    private var savedAlpha = 1f
    private var generation = 0L
    private var motion: ValueAnimator? = null
    private val size = activity.dp(42)
    private val margin = activity.dp(36)
    private val slop = activity.dp(6)
    private val threshold = activity.dp(30)
    private val hold = Runnable {
        if (!disposed && touching && axis == 0) {
            longDrag = true
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            view.alpha = .5f
        }
    }

    val view: ImageView = ImageView(activity).apply {
        layoutParams = FrameLayout.LayoutParams(size, size)
        isClickable = true
        isFocusable = true
        scaleType = ImageView.ScaleType.FIT_CENTER
        setPadding(activity.dp(10), activity.dp(10), activity.dp(10), activity.dp(10))
        setImageResource(R.drawable.ref_reveal_exit)
        setColorFilter(Color.WHITE)
        contentDescription = "退出全屏模式"
        background = StateListDrawable().apply {
            fun circle(color: Int) = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
            addState(intArrayOf(-android.R.attr.state_focused, android.R.attr.state_enabled, -android.R.attr.state_pressed), circle(0x99333333.toInt()))
            addState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed), circle(0x99666666.toInt()))
        }
        elevation = activity.dp(12).toFloat()
        visibility = View.GONE
        // Physical releases call the host after the Native rebound. TalkBack's direct click can reveal immediately.
        setOnClickListener { if (!disposed && !touching) host.revealToolbars() }
        setOnTouchListener { _, event -> onTouch(event) }
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) { updatePosition() }
            override fun onViewDetachedFromWindow(view: View) { resetMotion() }
        })
    }

    fun show(animated: Boolean = true) {
        if (disposed || visible) return
        visible = true
        view.animate().cancel()
        view.visibility = View.VISIBLE
        if (animated) {
            view.alpha = 0f
            view.animate().alpha(1f).translationY(0f).setDuration(160L).withEndAction(null).start()
        } else { view.alpha = 1f; view.translationY = 0f }
    }

    fun hide(animated: Boolean = true) {
        if (disposed || !visible) return
        visible = false
        resetMotion()
        view.animate().cancel()
        if (animated) {
            view.animate().translationY(activity.dp(32).toFloat()).alpha(0f).setDuration(160L)
                .withEndAction { if (!visible) view.visibility = View.GONE }.start()
        } else view.visibility = View.GONE
    }

    /** Call again after host recreation/configuration changes. Position uses absolute left/right, as in Native. */
    fun updatePosition() {
        if (disposed) return
        val position = host.position()
        val left = position and 3 == 3
        val right = !left && position and 5 == 5
        val bottom = !left && !right || position and 80 == 80
        val parent = view.parent
        view.layoutParams = if (parent is RelativeLayout) {
            RelativeLayout.LayoutParams(size, size).apply {
                when { left -> addRule(RelativeLayout.ALIGN_PARENT_LEFT); right -> addRule(RelativeLayout.ALIGN_PARENT_RIGHT); else -> addRule(RelativeLayout.CENTER_HORIZONTAL) }
                addRule(if (bottom) RelativeLayout.ALIGN_PARENT_BOTTOM else RelativeLayout.CENTER_VERTICAL)
                setMargins(margin, margin, margin, margin)
            }
        } else FrameLayout.LayoutParams(size, size).apply {
            gravity = (when { left -> Gravity.LEFT; right -> Gravity.RIGHT; else -> Gravity.CENTER_HORIZONTAL }) or
                if (bottom) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
            setMargins(margin, margin, margin, margin)
        }
    }

    private fun onTouch(event: MotionEvent): Boolean {
        if (disposed) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resetMotion()
                touching = true
                longDrag = false; axis = 0; direction = 0
                rawX = event.rawX.toInt(); rawY = event.rawY.toInt()
                downTime = SystemClock.elapsedRealtime()
                savedAlpha = view.alpha
                view.parent?.requestDisallowInterceptTouchEvent(true)
                view.postDelayed(hold, 300L)
            }
            MotionEvent.ACTION_MOVE -> move(event)
            // Native t0 treats cancellation as release and dispatches the rebound's armed action.
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.performClick()
                finish()
            }
        }
        return true
    }

    private fun move(event: MotionEvent) {
        if (!touching) return
        val dx = (event.rawX - rawX).toInt()
        val dy = (event.rawY - rawY).toInt()
        rawX = event.rawX.toInt(); rawY = event.rawY.toInt()
        if (longDrag) {
            view.translationX += dx
            view.translationY += dy
            return
        }
        if (axis == 0 && (abs(dx) > slop || abs(dy) > slop)) {
            axis = if (abs(dx) > abs(dy)) 3 else 12
            view.removeCallbacks(hold)
        }
        if (axis == 0) return
        if (axis == 3) {
            view.translationX = (view.translationX + (dx * .75f).toInt()).toInt().toFloat()
            view.translationY = 0f
        } else {
            view.translationX = 0f
            view.translationY = (view.translationY + (dy * .75f).toInt()).toInt().toFloat()
        }
        val next = when {
            abs(view.translationX) > threshold -> if (view.translationX <= 0) 1 else 2
            abs(view.translationY) > threshold -> if (view.translationY <= 0) 4 else 8
            else -> 0
        }
        preview(next)
    }

    private fun preview(value: Int) {
        if (direction == value) return
        direction = value
        view.setImageResource(when (value) {
            1 -> R.drawable.ref_reveal_back
            2 -> R.drawable.ref_reveal_forward
            4 -> R.drawable.ref_reveal_tabs
            8 -> R.drawable.ref_reveal_reload
            else -> R.drawable.ref_reveal_exit
        })
    }

    private fun finish() {
        if (!touching) return
        view.removeCallbacks(hold)
        val fromX = view.translationX.toInt()
        val fromY = view.translationY.toInt()
        val repositioning = longDrag
        val snap = if (repositioning) snap(rawX, rawY) else null
        if (repositioning) {
            view.alpha = savedAlpha
            snap?.let { host.savePosition(it.position) }
        }
        val toX = if (snap == null) 0 else snap.x - view.left
        val toY = if (snap == null) 0 else snap.y - view.top
        val dx = toX - fromX; val dy = toY - fromY
        val duration = (minOf(1f, maxOf(abs(dx), abs(dy)) / (slop * 30f)) * 150f + 100f).toLong()
        val token = ++generation
        motion?.cancel()
        motion = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = AccelerateInterpolator()
            addUpdateListener { value ->
                if (!disposed && token == generation) {
                    val fraction = value.animatedValue as Float
                    view.translationX = (dx * fraction + fromX).toInt().toFloat()
                    view.translationY = (dy * fraction + fromY).toInt().toFloat()
                }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (disposed || token != generation) return
                    motion = null
                    touching = false
                    view.parent?.requestDisallowInterceptTouchEvent(false)
                    if (repositioning) {
                        view.translationX = 0f; view.translationY = 0f
                        updatePosition()
                    } else {
                        val action = when (direction) { 1 -> Action.BACK; 2 -> Action.FORWARD; 4 -> Action.TABS; 8 -> Action.RELOAD; else -> null }
                        if (action != null) host.gesture(action)
                        else if (axis == 0 && SystemClock.elapsedRealtime() - downTime < 300L) host.revealToolbars()
                        preview(0)
                    }
                    axis = 0; longDrag = false
                }
            })
            start()
        }
    }

    private fun snap(x: Int, y: Int): Snap? {
        val parent = view.parent as? View ?: return null
        val width = parent.width; val height = parent.height
        if (width <= 0 || height <= 0) return null
        val bottomBoundary = height / 4 * 3 - margin
        val horizontal = when {
            x < width / 3 -> 3
            x > width / 3 * 2 -> 5
            y >= bottomBoundary -> 0
            x < width / 2 -> 3
            else -> 5
        }
        val bottom = horizontal == 0 || y >= bottomBoundary
        val targetX = when (horizontal) { 3 -> margin; 5 -> width - view.width - margin; else -> (width - view.width) / 2 }
        val targetY = if (bottom) height - view.height - margin else (height - view.height) / 2
        return Snap(targetX, targetY, horizontal or if (bottom) 80 else 0)
    }

    private fun resetMotion() {
        generation++
        motion?.cancel(); motion = null
        view.removeCallbacks(hold)
        if (longDrag) view.alpha = savedAlpha
        view.translationX = 0f
        if (touching) view.translationY = 0f
        touching = false; longDrag = false; axis = 0
        preview(0)
        view.parent?.requestDisallowInterceptTouchEvent(false)
    }

    fun dispose() {
        if (disposed) return
        disposed = true; visible = false
        resetMotion()
        view.animate().cancel()
        view.setOnTouchListener(null); view.setOnClickListener(null)
        view.visibility = View.GONE
    }
}
