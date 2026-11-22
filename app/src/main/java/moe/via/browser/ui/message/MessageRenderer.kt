package moe.via.browser.ui.message

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Typeface
import android.graphics.PorterDuff
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.components.recycler.bindNativeItemClick
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.surface
import moe.via.browser.ui.theme.palette

/** Native f6.h's single renderer, shared by normal, redirect and explicit dialog-parent notices. */
internal class MessageRenderer(activity: Activity, content: MessageContent, click: () -> Unit, detached: () -> Unit) {
    val body = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        setPaddingRelative(dp(16), 0, dp(16), 0)
        background = activity.surface(0xCC1E1E1E.toInt(), 30)
        contentDescription = content.text
    }
    private val animations = mutableSetOf<AnimatorSet>()
    private var action: TextView? = null
    private var disposed = false
    private val announcement = Runnable { if (!disposed) body.sendAccessibilityEvent(128) }
    private val detach = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) = Unit
        override fun onViewDetachedFromWindow(view: View) { if (!disposed) detached() }
    }
    val shown get() = body.isShown
    init {
        val textColor = if (activity.palette.night) 0xDEFFFFFF.toInt() else -1
        content.icon?.let { drawable ->
            body.addView(ImageView(activity).apply {
                setColorFilter(textColor, PorterDuff.Mode.SRC_IN); setImageDrawable(drawable)
            }, LinearLayout.LayoutParams(activity.dp(22), activity.dp(22)).apply { marginEnd = activity.dp(4) })
        }
        body.addView(NativeTypography.text(activity).apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
            setTextColor(textColor); maxWidth = dp(360); text = content.text
            setPaddingRelative(dp(4), dp(14), dp(4), dp(14))
            if (content.singleLine) { setSingleLine(); ellipsize = TextUtils.TruncateAt.END }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        content.action?.let { label ->
            val button = NativeTypography.text(activity).apply {
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
                setTextColor(activity.palette.accent); NativeTypography.style(this, android.graphics.Typeface.BOLD)
                minWidth = dp(56); maxWidth = dp(200); gravity = Gravity.CENTER; setAllCaps(true); text = label
                setPadding(dp(8), dp(8), dp(8), dp(8)); setBackgroundResource(R.drawable.ref_redirect_action_feedback)
                bindNativeItemClick(click)
                setOnLongClickListener { content.longClicked?.invoke() == true }
            }
            action = button; body.addView(button, LinearLayout.LayoutParams(-2, -2))
        }
        val base = content.bottomMarginPx.takeIf { it > 0 } ?: activity.dp(120)
        body.layoutParams = FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            setMargins(activity.dp(16), activity.dp(16), activity.dp(16), base)
        }
        body.addOnAttachStateChangeListener(detach)
        MessageInsets.install(body, base)
    }
    fun added() {
        NativeTypography.tree(body)
        animate(true)
        body.postDelayed(announcement, 30)
    }
    fun exit(removed: () -> Unit) = animate(false, removed)
    private fun animate(enter: Boolean, finished: (() -> Unit)? = null) {
        val animation = AnimatorSet()
        animations += animation
        animation.playTogether(ObjectAnimator.ofFloat(body, View.ALPHA, if (enter) 0f else 1f, if (enter) 1f else 0f),
            ObjectAnimator.ofFloat(body, View.TRANSLATION_Y, if (enter) 20f else 0f, if (enter) 0f else 20f))
        animation.duration = 180; animation.interpolator = DecelerateInterpolator()
        animation.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(value: Animator) { animations.remove(animation); if (!disposed) finished?.invoke() }
        })
        animation.start()
    }
    fun dispose() {
        if (disposed) return
        disposed = true
        animations.toList().forEach { it.removeAllListeners(); it.cancel() }; animations.clear()
        body.removeCallbacks(announcement); body.removeOnAttachStateChangeListener(detach)
        body.setOnApplyWindowInsetsListener(null)
        action?.setOnClickListener(null); action?.setOnLongClickListener(null); action = null
    }
}
