package moe.pure.browser.feature.translation.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import moe.pure.browser.ui.components.dp
import kotlin.math.abs
import kotlin.random.Random

/** Simulated request progress, matching the reference's 10x scale and fading completion. */
internal class TranslationProgress(private val bar: ProgressBar) {
    private val maximum = 1000
    private val ceiling = 800
    private var running = false
    private var animator: ValueAnimator? = null

    init {
        bar.visibility = View.GONE
        bar.max = maximum
        val clear = GradientDrawable().apply { setColor(0) }
        val fill = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(0xFF59C2E5.toInt(), 0xFFA687E0.toInt())).apply {
            cornerRadius = bar.dp(2).toFloat()
        }
        bar.progressDrawable = LayerDrawable(arrayOf(clear,
            ClipDrawable(fill, Gravity.LEFT, ClipDrawable.HORIZONTAL))).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
        bar.progressTintList = null
        bar.progressBackgroundTintList = null
    }

    fun begin() = update(Random.nextInt(10, 45) * 10, true)
    fun finish() = update(maximum, false)

    fun dispose() {
        animator?.removeAllListeners()
        animator?.cancel()
        animator = null
        running = false
        bar.alpha = 1f
        bar.progress = 0
        bar.visibility = View.GONE
    }

    private fun update(target: Int, restart: Boolean) {
        if (!running && target < maximum) {
            bar.alpha = 1f
            bar.visibility = View.VISIBLE
            running = true
        }
        if (!running) return
        val initial = bar.progress
        if (target < maximum && initial != 0 && abs(target - initial) < maximum / 8) return
        animator?.removeAllListeners()
        animator?.cancel()
        bar.alpha = 1f
        var duration = abs(initial - target).toLong()
        val animation = ValueAnimator.ofFloat(0f, 1f)
        if (target < ceiling) {
            if (restart || initial < target) {
                duration += abs(ceiling - target) * 30L
                val ratio = duration.toFloat() / abs(initial - target).toFloat()
                animation.addUpdateListener {
                    val value = it.animatedValue as Float
                    bar.progress = (initial + minOf(ratio * value, 1f) * (target - initial) +
                        value * abs(ceiling - target)).toInt()
                }
            } else {
                duration = abs(ceiling - initial) * 30L
                animation.addUpdateListener {
                    val value = it.animatedValue as Float
                    bar.progress = (initial + value * value * abs(ceiling - initial)).toInt()
                }
            }
        } else if (target < maximum) {
            animation.addUpdateListener {
                bar.progress = (initial + (it.animatedValue as Float) * (target - initial)).toInt()
            }
        } else {
            duration /= 2
            animation.addUpdateListener {
                val value = it.animatedValue as Float
                bar.progress = (initial + (maximum - initial) * value).toInt()
                bar.alpha = 1f - value
            }
            animation.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    bar.visibility = View.GONE
                    bar.progress = 0
                    running = false
                }
            })
        }
        animator = animation
        animation.interpolator = DecelerateInterpolator()
        animation.duration = duration
        animation.start()
    }
}
