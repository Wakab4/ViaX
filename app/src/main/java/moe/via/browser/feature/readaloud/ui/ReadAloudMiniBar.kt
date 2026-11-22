package moe.via.browser.feature.readaloud.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** The page's left edge control. Position is owned by the browser's controller. */
class ReadAloudMiniBar(context: Context, private val actions: Actions) : LinearLayout(context) {
    interface Actions {
        fun panel()
        fun play()
        fun pause()
        fun stop()
    }
    private val article = button(R.drawable.ref_ic_menu_read_aloud, "朗读网页", 6) { actions.panel() }
    private val toggle = button(R.drawable.ref_tts_play_initial, "恢复", 7) { if (playing) actions.pause() else actions.play() }
    private val stop = button(R.drawable.ref_tts_stop, "停止", 7) { actions.stop() }
    private var spin: ObjectAnimator? = null
    private var playing = false
    private var downX = 0f
    private var downY = 0f

    init {
        orientation = HORIZONTAL
        background = GradientDrawable().apply {
            setColor(context.palette.panel)
            val radius = dp(18).toFloat()
            cornerRadii = floatArrayOf(0f, 0f, radius, radius, radius, radius, 0f, 0f)
        }
        addView(article); addView(toggle); addView(stop)
    }

    fun setPlaying(value: Boolean) {
        if (playing == value) return
        playing = value
        if (value) startAnimation() else { spin?.cancel(); spin = null }
        toggle.setImageResource(if (value) R.drawable.ref_tts_pause else R.drawable.ref_tts_play)
        toggle.contentDescription = if (value) "暂停" else "恢复"
        stop.visibility = if (value) View.GONE else View.VISIBLE
    }

    private fun startAnimation() {
        if (spin != null || !isAttachedToWindow) return
        spin = ObjectAnimator.ofFloat(article, View.ROTATION, article.rotation, article.rotation + 360f).apply {
            duration = 3000; repeatCount = ObjectAnimator.INFINITE; interpolator = LinearInterpolator(); start()
        }
    }

    fun constrainPosition() {
        val container = parent as? ViewGroup ?: return
        if (container.height <= 0) return
        val margin = dp(88).toFloat()
        val maximum = (container.height - height - margin).coerceAtLeast(0f)
        translationY = translationY.coerceIn(minOf(margin, maximum), maximum)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) { downX = event.rawX; downY = event.rawY }
        else if (event.actionMasked == MotionEvent.ACTION_MOVE && kotlin.math.abs(event.rawY - downY) > kotlin.math.abs(event.rawX - downX)) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (parent == null) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY }
            MotionEvent.ACTION_MOVE -> {
                translationY += event.rawY - downY; constrainPosition()
                downX = event.rawX; downY = event.rawY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }

    private fun button(icon: Int, label: String, padding: Int, action: () -> Unit) = ImageView(context).apply {
        setImageResource(icon); contentDescription = label
        imageTintList = ColorStateList.valueOf(context.palette.icon)
        setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
        layoutParams = LayoutParams(dp(36), dp(36)); setOnClickListener { action() }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); if (playing) startAnimation() }
    override fun onDetachedFromWindow() { spin?.cancel(); spin = null; super.onDetachedFromWindow() }
}
