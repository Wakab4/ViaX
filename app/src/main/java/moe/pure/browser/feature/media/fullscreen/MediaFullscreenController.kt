package moe.pure.browser.feature.media.fullscreen

import moe.pure.browser.ui.message.BrowserMessages

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import moe.pure.browser.feature.media.data.MediaPreferences
import moe.pure.browser.feature.media.model.MediaGestureKind
import moe.pure.browser.feature.media.model.MediaMetadata
import moe.pure.browser.feature.media.model.MediaOverlayState
import moe.pure.browser.feature.media.pip.MediaPipController
import moe.pure.browser.feature.media.ui.MediaActions
import moe.pure.browser.feature.media.ui.MediaOverlay
import moe.pure.browser.feature.media.web.MediaPageController
import moe.pure.browser.feature.media.web.MediaSitePolicy

/** One HTML custom view per Activity. The original WebView and its document remain alive. */
class MediaFullscreenController(
    private val activity: Activity,
    private val callbacks: Callbacks = Callbacks()
) {
    data class Callbacks(
        val onFullscreenChanged: (Boolean) -> Unit = {},
        val allowGestures: () -> Boolean = { true },
        val allowToolbar: () -> Boolean = { true },
        val gesturesEnabled: (() -> Boolean)? = null,
        val message: ((String) -> Unit)? = null
    )

    private class Session(
        val tabId: String,
        val webView: WebView,
        val customView: View,
        val callback: WebChromeClient.CustomViewCallback,
        val overlay: MediaOverlay,
        val page: MediaPageController,
        val window: MediaWindowPolicy,
        val gestures: MediaGestureController,
        val originalVisibility: Int,
        val startedAt: Long,
        var metadata: MediaMetadata = MediaMetadata(),
        var state: MediaOverlayState = MediaOverlayState(),
        var pip: MediaPipController? = null,
        var enteringPip: Boolean = false
    )

    private val handler = Handler(Looper.getMainLooper())
    private val preferences = MediaPreferences(activity)
    private var session: Session? = null
    private var disposed = false
    private var earlyExitCount = 0

    val active: Boolean get() = session != null
    val sourceTabId: String? get() = session?.tabId
    val sourceWebView: WebView? get() = session?.webView
    val isInPipMode: Boolean get() = session?.state?.inPipMode == true || activity.isInPictureInPictureMode
    /** Native pauses a normal background fullscreen page, but keeps the PiP document running. */
    val keepWebViewResumed: Boolean get() = session?.let { it.enteringPip || it.state.inPipMode } == true || activity.isInPictureInPictureMode

    fun show(sourceTabId: String, sourceWebView: WebView, customView: View, callback: WebChromeClient.CustomViewCallback) {
        checkMain()
        if (disposed || session != null || activity.isFinishing || activity.isDestroyed) {
            runCatching { callback.onCustomViewHidden() }; return
        }
        // GP resets a repeatedly rejected forced orientation after two sub-800 ms exits.
        if (earlyExitCount > 1) preferences.videoOrientation = 0
        val policy = MediaWindowPolicy(activity)
        val overlay = MediaOverlay(activity, actions)
        val current = Session(sourceTabId, sourceWebView, customView, callback, overlay,
            MediaPageController(activity, sourceWebView), policy, MediaGestureController(activity),
            sourceWebView.visibility, SystemClock.elapsedRealtime())
        current.state = MediaOverlayState(title = sourceWebView.title.orEmpty(), toolbarEnabled = false,
            orientation = preferences.videoOrientation, orientationAdjustable = policy.orientationAdjustable,
            canEnterPip = activity.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE))
        val pip = MediaPipController(activity, rewind = { currentSession(current)?.page?.seek(-15) },
            toggle = { currentSession(current)?.page?.toggle { state -> currentSession(current)?.pip?.playing(state) } },
            fastForward = { currentSession(current)?.page?.seek(15) })
        current.pip = pip
        session = current
        try {
            (customView.parent as? ViewGroup)?.removeView(customView)
            customView.keepScreenOn = true
            overlay.root.setBackgroundColor(Color.BLACK)
            overlay.root.addView(customView, 0, FrameLayout.LayoutParams(-1, -1))
            val decor = activity.window.decorView as? ViewGroup ?: error("Custom view requires an Activity decor")
            decor.addView(overlay.root, ViewGroup.LayoutParams(-1, -1))
            sourceWebView.visibility = View.INVISIBLE
            overlay.render(current.state)
            callbacks.onFullscreenChanged(true)
            policy.immersive()
            pip.attach()
            overlay.root.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                if (session === current) current.pip?.position(view)
            }
            handler.postDelayed({
                if (session === current && !disposed) current.page.metadata { metadata ->
                    if (session === current && !disposed) {
                        current.metadata = metadata
                        renderPolicy(current)
                        current.pip?.dimensions(metadata.width, metadata.height)
                        applyOrientation(current)
                    }
                }
            }, 150L)
        } catch (_: RuntimeException) { hide() }
    }

    fun hide() {
        checkMain()
        val current = session ?: return
        session = null
        earlyExitCount = if (SystemClock.elapsedRealtime() - current.startedAt < 800L) earlyExitCount + 1 else 0
        current.page.dispose()
        current.pip?.dispose()
        current.customView.keepScreenOn = false
        current.gestures.dispose()
        current.overlay.dispose()
        (current.overlay.root.parent as? ViewGroup)?.removeView(current.overlay.root)
        current.overlay.root.removeAllViews()
        current.webView.visibility = current.originalVisibility
        current.window.restore()
        runCatching { current.callback.onCustomViewHidden() }
        callbacks.onFullscreenChanged(false)
    }

    fun onBack(): Boolean {
        checkMain()
        if (!active) return false
        hide()
        return true
    }

    fun navigation(tabId: String) { checkMain(); if (session?.tabId == tabId) hide() }
    fun tabReleased(tabId: String, source: WebView? = null) {
        checkMain()
        if (session?.tabId == tabId && (source == null || session?.webView === source)) hide()
    }

    fun onResume() {
        checkMain()
        session?.let { current ->
            current.enteringPip = false
            current.webView.onResume()
            renderPolicy(current)
            applyOrientation(current)
            if (!isInPipMode) current.window.immersive()
        }
    }

    fun onPause() { checkMain() /* The host owns WebView pause and consults keepWebViewResumed. */ }

    fun onPictureInPictureModeChanged(inPipMode: Boolean) {
        checkMain()
        session?.let { current ->
            current.enteringPip = false
            current.state = current.state.copy(inPipMode = inPipMode, gestureHud = null)
            current.overlay.render(current.state)
            if (inPipMode) current.webView.onResume()
            else {
                // GP explicitly pauses the page when leaving PiP; Activity resume reactivates it.
                current.webView.onPause()
                current.window.immersive()
                applyOrientation(current)
            }
        }
    }

    fun onConfigurationChanged(configuration: Configuration) {
        checkMain()
        session?.let { current ->
            renderPolicy(current)
            applyOrientation(current)
            current.overlay.root.requestApplyInsets()
            if (!isInPipMode) current.window.immersive()
        }
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        checkMain()
        if (hasFocus && !isInPipMode) session?.window?.immersive()
    }

    private fun renderPolicy(current: Session) {
        if (session !== current) return
        val policy = MediaSitePolicy.from(current.webView.url)
        val metadata = current.metadata
        val gestures = callbacks.allowGestures() && (callbacks.gesturesEnabled?.invoke() ?: preferences.gesturesEnabled)
        current.state = current.state.copy(
            orientation = preferences.videoOrientation,
            orientationAdjustable = current.window.orientationAdjustable,
            toolbarEnabled = metadata.available && callbacks.allowToolbar() && policy.toolbar,
            speed = metadata.speed,
            speedAdjustable = metadata.finiteDuration && policy.seekAndSpeed,
            progressGestureEnabled = metadata.finiteDuration && gestures && policy.seekAndSpeed,
            brightnessGestureEnabled = metadata.available && gestures && policy.volumeAndBrightness,
            volumeGestureEnabled = metadata.available && gestures && policy.volumeAndBrightness
        )
        current.overlay.render(current.state)
    }

    private fun applyOrientation(current: Session) {
        if (session === current && !current.state.inPipMode && !current.enteringPip)
            current.window.orientation(current.state.orientation, current.metadata.width > current.metadata.height, current.state.locked)
    }

    private fun currentSession(expected: Session) = session?.takeIf { it === expected }

    private val actions = object : MediaActions {
        override fun onLockToggle() {
            val current = session ?: return
            current.state = current.state.copy(locked = !current.state.locked, gestureHud = null)
            current.overlay.render(current.state)
            applyOrientation(current)
        }

        override fun onSpeed(anchor: View) {
            val current = session ?: return
            if (!current.state.speedAdjustable || current.state.locked) return
            current.overlay.showSpeedMenu(current.state.speed) { rate ->
                if (session === current && current.state.speedAdjustable) {
                    current.page.speed(rate)
                    current.metadata = current.metadata.copy(speed = rate)
                    current.state = current.state.copy(speed = rate)
                    current.overlay.render(current.state)
                }
            }
        }

        override fun onOrientation(anchor: View) {
            val current = session ?: return
            if (!current.state.orientationAdjustable || current.state.locked) return
            current.overlay.showOrientationMenu(current.state.orientation) { selected ->
                if (session === current) {
                    preferences.videoOrientation = selected
                    current.state = current.state.copy(orientation = preferences.videoOrientation)
                    current.overlay.render(current.state)
                    applyOrientation(current)
                }
            }
        }

        override fun onPip() {
            val current = session ?: return
            val pip = current.pip ?: return
            if (!current.state.canEnterPip || current.state.locked) return
            if (!pip.permitted()) { message("没有权限"); return }
            current.enteringPip = true
            current.state = current.state.copy(inPipMode = true, gestureHud = null)
            current.overlay.render(current.state)
            if (!pip.enter()) {
                current.enteringPip = false
                current.state = current.state.copy(inPipMode = false)
                current.overlay.render(current.state)
                return
            }
            if (current.metadata.finiteDuration) current.page.playing { value -> if (session === current) pip.playing(value) }
        }

        override fun onGestureStart(kind: MediaGestureKind) {
            val current = session ?: return
            if (gestureAllowed(current, kind)) current.gestures.start(kind)
        }

        override fun onGestureMove(kind: MediaGestureKind, fraction: Float) {
            val current = session ?: return
            if (!gestureAllowed(current, kind)) return
            current.gestures.move(kind, fraction, current.metadata.duration)?.let {
                current.state = current.state.copy(gestureHud = it)
                current.overlay.render(current.state)
            }
        }

        override fun onGestureEnd(kind: MediaGestureKind, cancelled: Boolean) {
            val current = session ?: return
            current.gestures.end(kind, cancelled)?.let { current.page.seek(it) }
            current.state = current.state.copy(gestureHud = null)
            current.overlay.render(current.state)
        }
    }

    private fun gestureAllowed(current: Session, kind: MediaGestureKind): Boolean = !current.state.locked && !current.state.inPipMode && when (kind) {
        MediaGestureKind.BRIGHTNESS -> current.state.brightnessGestureEnabled
        MediaGestureKind.VOLUME -> current.state.volumeGestureEnabled
        MediaGestureKind.SEEK -> current.state.progressGestureEnabled
    }

    private fun message(text: String) {
        callbacks.message?.invoke(text) ?: BrowserMessages.show(activity, text)
    }

    fun dispose() {
        checkMain()
        if (disposed) return
        hide()
        handler.removeCallbacksAndMessages(null)
        disposed = true
    }

    private fun checkMain() = check(Looper.myLooper() == Looper.getMainLooper()) { "HTML media belongs to the browser main thread" }
}
