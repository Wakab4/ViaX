package moe.via.browser.browser.ui.chrome.address

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.PathInterpolator
import java.lang.ref.WeakReference

/** n0.q/l/c: restarting the 1200ms pulse cancels the previous animator and resets scale. */
internal class ReaderHintAnimation(private val icon: View) {
    private var reference: WeakReference<ValueAnimator>? = null
    fun start() {
        cancel()
        val animator = ValueAnimator.ofFloat(1f, 1.15f, 1f).apply {
            addUpdateListener { icon.scaleX = it.animatedValue as Float; icon.scaleY = it.animatedValue as Float }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationCancel(animation: Animator) { icon.scaleX = 1f; icon.scaleY = 1f }
            })
            interpolator = PathInterpolator(.2f, .2f, .8f, .8f)
            duration = 1200
        }
        animator.start()
        reference = WeakReference(animator)
    }
    fun cancel() { reference?.get()?.cancel(); reference = null }
}
