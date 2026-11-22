package moe.via.browser.browser.ui.chrome.progress

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.SystemClock
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import kotlin.math.abs

/**
 * GP widget.w's 1000-step progress animation. Inputs above 100 deliberately retain their
 * extra distance in the completion duration: browser input 120 and document input 100 differ.
 */
internal class NativePageProgress(private val bar: ProgressBar) {
    private var animator: ValueAnimator? = null
    private var activeMotion: NativeProgressMotion? = null
    private var showing = false
    private var disposed = false
    private var generation = 0L

    init {
        bar.isIndeterminate = false
        bar.max = MAXIMUM
        bar.progress = 0
        bar.visibility = View.GONE
        NativeProgressDrawable.applyTo(bar)
    }

    /** [towardTarget] is widget.w.g's optional branch; its ordinary consumers pass false. */
    fun update(percent: Int, towardTarget: Boolean = false) {
        if (disposed) return
        val target = percent * 10
        if (!showing && target < MAXIMUM) {
            bar.alpha = 1f
            bar.visibility = View.VISIBLE
            showing = true
        }
        if (!showing) return
        val initial = bar.progress
        if (target < MAXIMUM && initial != 0 && abs(target - initial) < MAXIMUM / 8) return
        animate(initial, target, towardTarget)
    }

    fun capture(): NativeProgressState {
        val motion = activeMotion?.let { specification ->
            animator?.takeIf { it.isStarted }?.let {
                specification.copy(playTime = it.currentPlayTime, capturedAt = SystemClock.uptimeMillis())
            }
        }
        return NativeProgressState(bar.progress, bar.alpha, showing, motion)
    }

    fun restore(state: NativeProgressState) {
        if (disposed) return
        cancelAnimation()
        showing = state.showing
        bar.progress = state.progress
        bar.alpha = state.alpha
        bar.visibility = if (showing) View.VISIBLE else View.GONE
        if (showing) state.motion?.let { motion ->
            val scale = durationScale()
            val elapsed = if (scale <= 0f) Long.MAX_VALUE else motion.playTime +
                ((SystemClock.uptimeMillis() - motion.capturedAt).coerceAtLeast(0L) / scale).toLong()
            animate(motion.initial, motion.target, motion.towardTarget, elapsed)
        }
    }

    private fun animate(initial: Int, target: Int, towardTarget: Boolean, resumeAt: Long? = null) {
        cancelAnimation()
        val token = generation
        bar.alpha = 1f
        val difference = abs(initial - target).toLong()
        val duration: Long
        val frame: (Float) -> Unit
        when {
            target < PLATEAU && (towardTarget || initial < target) -> {
                duration = abs(PLATEAU - target) * 30L + difference
                // DEX w.e:005e..0060 converts both longs to float before div-float.
                val ratio = duration.toFloat() / difference.toFloat()
                frame = { fraction ->
                    bar.progress = (initial + minOf(ratio * fraction, 1f) * (target - initial) +
                        fraction * abs(PLATEAU - target)).toInt()
                }
            }
            target < PLATEAU -> {
                duration = abs(PLATEAU - initial) * 30L
                frame = { fraction ->
                    bar.progress = (initial.toDouble() + Math.pow(fraction.toDouble(), 2.0) * abs(PLATEAU - initial)).toInt()
                }
            }
            target < MAXIMUM -> {
                duration = difference
                frame = { fraction -> bar.progress = (initial + fraction * (target - initial)).toInt() }
            }
            else -> {
                duration = difference / 2
                frame = { fraction ->
                    bar.progress = (initial + fraction * (MAXIMUM - initial)).toInt()
                    bar.alpha = 1f - fraction
                }
            }
        }
        if (resumeAt != null && resumeAt >= duration) {
            frame(1f)
            if (target >= MAXIMUM) complete()
            return
        }
        val motion = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { if (!disposed && token == generation) frame(it.animatedValue as Float) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (disposed || token != generation) return
                    animator = null
                    activeMotion = null
                    if (target >= MAXIMUM) complete()
                }
            })
        }
        activeMotion = NativeProgressMotion(initial, target, towardTarget, 0L, 0L)
        animator = motion
        motion.start()
        if (resumeAt != null && resumeAt > 0L) motion.currentPlayTime = resumeAt
    }

    /** Only disposal or an unavailable document hides immediately; normal completion uses update(). */
    fun hide() {
        cancelAnimation()
        showing = false
        bar.visibility = View.GONE
        bar.progress = 0
    }

    fun dispose() { if (!disposed) { disposed = true; hide() } }

    private fun cancelAnimation() {
        generation++
        animator?.removeAllListeners()
        animator?.removeAllUpdateListeners()
        animator?.cancel()
        animator = null
        activeMotion = null
    }

    private fun complete() {
        bar.visibility = View.GONE
        bar.progress = 0
        showing = false
    }

    private fun durationScale(): Float = if (Build.VERSION.SDK_INT >= 33) ValueAnimator.getDurationScale() else
        runCatching { Settings.Global.getFloat(bar.context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) }.getOrDefault(1f)

    companion object {
        private const val MAXIMUM = 1000
        private const val PLATEAU = 800
    }
}
