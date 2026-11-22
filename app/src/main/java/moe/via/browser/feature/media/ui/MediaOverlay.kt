package moe.via.browser.feature.media.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import moe.via.browser.R
import moe.via.browser.feature.media.model.MediaGestureKind
import moe.via.browser.feature.media.model.MediaOverlayState
import moe.via.browser.ui.components.dp

/** Contains the page custom view at index 0 and the reference's extra video affordances above it. */
class MediaOverlay(private val activity: Activity, private val actions: MediaActions) {
    private var state = MediaOverlayState()
    private var disposed = false
    private var controlsVisible = false
    private val visibilityTargets = java.util.IdentityHashMap<View, Boolean>()
    private val interpolator = PathInterpolator(.2f, .2f, .8f, .8f)
    private val hideControls = Runnable { hideToolbar() }
    private val touch = MediaTouchDispatcher(actions, { state }, activity::dp, ::toggleToolbar, ::hideHud)
    val root: FrameLayout = object : FrameLayout(activity) {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if (!disposed) touch.observe(event, width, height)
            return super.dispatchTouchEvent(event)
        }
    }
    private val views = MediaOverlayViews(activity, root)
    private val menus = MediaMenus(activity, ::holdToolbar, ::showToolbar)

    init {
        root.layoutParams = FrameLayout.LayoutParams(-1, -1)
        views.lock.setOnClickListener {
            holdToolbar(); actions.onLockToggle(); showToolbar()
        }
        views.speed.setOnClickListener { holdToolbar(); actions.onSpeed(it) }
        views.orientation.setOnClickListener { holdToolbar(); actions.onOrientation(it) }
        views.pip.setOnClickListener { actions.onPip() }
        views.controls.setOnApplyWindowInsetsListener { view, insets ->
            applyInsets(view, insets); insets
        }
        views.controls.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            view.rootWindowInsets?.let { applyInsets(view, it) }
        }
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) { view.requestApplyInsets() }
            override fun onViewDetachedFromWindow(view: View) {
                holdToolbar(); menus.dismiss(); hideHud()
            }
        })
    }

    fun render(value: MediaOverlayState) {
        if (disposed) return
        val previous = state
        state = value
        views.title.text = value.title
        views.speedLabel.text = "${value.speed}X"
        views.speed.contentDescription = "播放速度，当前 ${value.speed} 倍"
        views.lock.setImageResource(if (value.locked) R.drawable.ref_media_lock else R.drawable.ref_media_unlock)
        views.lock.contentDescription = if (value.locked) "解锁" else "锁定"
        if (previous.toolbarEnabled != value.toolbarEnabled || value.inPipMode) {
            holdToolbar(); hideToolbar()
        } else if (previous.inPipMode && !value.inPipMode) {
            controlsVisible = true
            applyToolbarVisibility()
        } else if (controlsVisible) {
            applyToolbarVisibility()
        }
        value.gestureHud?.let { hud ->
            views.hud.text = hud.text
            val icon = when (hud.kind) {
                MediaGestureKind.BRIGHTNESS -> R.drawable.ref_media_brightness
                MediaGestureKind.VOLUME -> R.drawable.ref_media_volume
                MediaGestureKind.SEEK -> 0
            }
            val drawable = if (icon == 0) null else activity.getDrawable(icon)?.mutate()?.apply {
                setTint(Color.WHITE)
                setBounds(0, 0, activity.dp(24), activity.dp(24))
            }
            views.hud.setCompoundDrawablesRelative(drawable, null, null, null)
            fade(views.hud, true)
        } ?: hideHud()
    }

    fun showSpeedMenu(current: Float, onSelected: (Float) -> Unit) =
        showSpeedMenu(views.speed, current, onSelected)

    fun showSpeedMenu(anchor: View, current: Float, onSelected: (Float) -> Unit) {
        if (!disposed) menus.speed(anchor, current, onSelected)
    }

    fun showOrientationMenu(current: Int, onSelected: (Int) -> Unit) =
        showOrientationMenu(views.orientation, current, onSelected)

    fun showOrientationMenu(anchor: View, current: Int, onSelected: (Int) -> Unit) {
        if (!disposed) menus.orientation(anchor, current, onSelected)
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        holdToolbar(); menus.dismiss()
        views.all.forEach { it.animate().cancel() }
        views.controls.setOnApplyWindowInsetsListener(null)
    }

    private fun toggleToolbar() {
        if (controlsVisible) { holdToolbar(); hideToolbar() }
        else if (state.toolbarEnabled && !state.inPipMode) showToolbar()
    }

    private fun showToolbar() {
        if (disposed || !state.toolbarEnabled || state.inPipMode) return
        controlsVisible = true
        holdToolbar()
        root.postDelayed(hideControls, 3_000)
        applyToolbarVisibility()
    }

    private fun holdToolbar() { root.removeCallbacks(hideControls) }

    private fun applyToolbarVisibility() {
        val unlocked = !state.locked
        fade(views.lock, true)
        fade(views.title, unlocked && state.title.isNotEmpty())
        fade(views.gradient, unlocked && state.title.isNotEmpty())
        fade(views.speed, unlocked && state.speedAdjustable)
        fade(views.orientation, unlocked && state.orientationAdjustable)
        fade(views.pip, unlocked && state.canEnterPip)
    }

    private fun hideToolbar() {
        controlsVisible = false
        views.toolbar.forEach { fade(it, false) }
    }

    private fun hideHud() { fade(views.hud, false) }

    private fun fade(view: View, visible: Boolean) {
        if (visibilityTargets.put(view, visible) == visible) return
        if (visible) {
            view.animate().cancel()
            if (view.visibility != View.VISIBLE) view.alpha = 0f
            view.visibility = View.VISIBLE
            view.animate().alpha(1f).setInterpolator(interpolator).setDuration(150).withEndAction(null).start()
        } else {
            if (view.visibility != View.VISIBLE) return
            view.animate().cancel()
            view.animate().alpha(0f).setInterpolator(interpolator).setDuration(150)
                .withEndAction { view.visibility = View.GONE }.start()
        }
    }

    private fun applyInsets(view: View, insets: WindowInsets) {
        val cutout = insets.displayCutout
        if (state.canEnterPip) {
            view.setPadding(0, 0, cutout?.safeInsetRight ?: 0, 0)
            val middle = view.height / 2
            var offset = 0
            cutout?.boundingRects?.forEach { rect ->
                if (rect.contains(activity.dp(16), middle)) offset = maxOf(offset, rect.bottom - middle + activity.dp(32))
            }
            val params = views.pip.layoutParams as RelativeLayout.LayoutParams
            if (params.topMargin != offset) {
                params.topMargin = offset; views.pip.layoutParams = params
            }
        } else {
            @Suppress("DEPRECATION")
            view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        }
    }

    companion object {
        /** GP res/layout/a.xml: platform large spinner, centered, without an opaque backdrop. */
        fun loadingView(context: Context): View = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(-1, -1)
            addView(ProgressBar(context, null, android.R.attr.progressBarStyleLarge),
                FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        }
    }
}
