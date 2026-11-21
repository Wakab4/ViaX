package moe.pure.browser.feature.search.ui

import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

/** The reference address and expanded fragments use 240ms alpha transitions. */
internal object SearchTransitions {
    fun show(view: View) {
        view.startAnimation(AlphaAnimation(0f, 1f).apply { duration = 240L })
    }

    fun remove(view: View, animate: Boolean) {
        val parent = view.parent as? ViewGroup ?: return
        if (!animate || !view.isAttachedToWindow) { parent.removeView(view); return }
        parent.startViewTransition(view)
        view.startAnimation(AlphaAnimation(1f, 0f).apply {
            duration = 240L
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) = Unit
                override fun onAnimationRepeat(animation: Animation) = Unit
                override fun onAnimationEnd(animation: Animation) { parent.endViewTransition(view) }
            })
        })
        parent.removeView(view)
    }
}
