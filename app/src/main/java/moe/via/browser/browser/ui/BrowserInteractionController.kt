package moe.via.browser.browser.ui

import android.app.Activity
import android.content.res.Configuration
import android.net.http.SslError
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.browser.ui.gestures.BrowserActionDispatcher
import moe.via.browser.browser.ui.gestures.BrowserInputCallbacks
import moe.via.browser.browser.ui.gestures.BrowserInputController
import moe.via.browser.browser.ui.gestures.BrowserInputState
import moe.via.browser.browser.ui.gestures.ToolbarRevealButton
import moe.via.browser.feature.media.fullscreen.MediaFullscreenController
import moe.via.browser.feature.media.ui.MediaOverlay
import moe.via.browser.storage.preferences.NativeAction
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.browser.ui.chrome.BrowserChrome
import moe.via.browser.browser.page.ssl.BrowserSslController

/** Coordinates page input and custom views without owning session data or feature dialogs. */
class BrowserInteractionController(private val activity: Activity, private val settings: BrowserSettings, private val host: Host) {
    interface Host : BrowserActionDispatcher.Host {
        fun chrome(): BrowserChrome
        fun inputState(): BrowserInputState
        fun dismissOverlay(): Boolean
        fun showMenu()
        fun showTabs()
        fun goHome()
        fun restoreWindow()
        fun newTabAndAddress()
    }
    private val actions = BrowserActionDispatcher(host)
    private var windowFocused = true
    private val ssl = BrowserSslController(activity, settings.preferences)
    private val webDialogs = moe.via.browser.browser.page.dialog.BrowserWebDialogs(activity)
    private val media = MediaFullscreenController(activity, MediaFullscreenController.Callbacks(
        onFullscreenChanged = ::fullscreenChanged,
        allowGestures = { !host.inputState().gameMode },
        allowToolbar = { !host.inputState().gameMode },
        gesturesEnabled = { settings.preferences.videoGestures }
    ))
    val input: BrowserInputController = BrowserInputController(activity, settings.preferences, object : BrowserInputCallbacks {
        override fun state() = host.inputState().copy(customViewShowing = media.active)
        override fun execute(action: NativeAction) = actions.execute(action)
        override fun dismissOverlay() = host.dismissOverlay()
        override fun showMenu() = host.showMenu()
        override fun goHome() = host.goHome()
        override fun selectTab(index: Int) = host.select(if (index < 0) host.tabCount() - 1 else index)
        override fun findNext(forward: Boolean) { host.current()?.liveView?.findNext(forward) }
        override fun newTabAndAddress() = host.newTabAndAddress()
        override fun setChromeVisible(visible: Boolean) = changeChromeVisibility(visible)
        override fun onToolbarPreview(direction: Int) = host.chrome().setToolbarPreview(direction)
    })
    private val reveal: ToolbarRevealButton by lazy { ToolbarRevealButton(activity, object : ToolbarRevealButton.Host {
        override fun revealToolbars() = changeChromeVisibility(true)
        override fun gesture(action: ToolbarRevealButton.Action) {
            when (action) {
                ToolbarRevealButton.Action.BACK -> actions.execute(NativeAction.BACK)
                ToolbarRevealButton.Action.FORWARD -> actions.execute(NativeAction.FORWARD)
                ToolbarRevealButton.Action.TABS -> host.showTabs()
                ToolbarRevealButton.Action.RELOAD -> actions.execute(NativeAction.RELOAD)
            }
        }
        override fun position() = settings.preferences.int("fab", 80)
        override fun savePosition(position: Int) { settings.values.edit().putInt("fab", position).apply() }
    }) }
    val customViewShowing get() = media.active
    private fun fullscreenChanged(active: Boolean) {
        input.reset()
        host.chrome().root.visibility = if (active) View.INVISIBLE else View.VISIBLE
        if (!active) host.restoreWindow()
        updateChromeControls()
    }
    fun rebind() {
        input.reset()
        host.chrome().refreshViewportPolicy()
        host.chrome().attachGestureIndicator(input.pageIndicator)
        host.chrome().attachRevealButton(reveal.view)
        host.chrome().root.visibility = if (media.active) View.INVISIBLE else View.VISIBLE
        updateChromeControls()
    }
    fun selected(tab: BrowserTab) {
        webDialogs.selected(tab.liveView)
        if (media.sourceTabId != null && (media.sourceTabId != tab.id || media.sourceWebView !== tab.liveView)) media.hide()
        input.reset()
        changeChromeVisibility(true)
    }
    fun navigation(tab: BrowserTab) {
        media.navigation(tab.id)
        tab.liveView?.let(ssl::navigation)
        tab.liveView?.let(webDialogs::navigation)
        if (tab === host.current()) input.reset()
    }
    fun released(tab: BrowserTab, view: WebView) { media.tabReleased(tab.id, view); ssl.navigation(view); webDialogs.navigation(view) }
    fun pageTouch(tab: BrowserTab, view: WebView, event: MotionEvent): Boolean =
        tab === host.current() && tab.isCurrent(view) && input.onTouch(view, event)
    fun showCustomView(tab: BrowserTab, source: WebView, view: View, callback: WebChromeClient.CustomViewCallback) {
        if (tab !== host.current() || !tab.isCurrent(source)) { callback.onCustomViewHidden(); return }
        host.dismissOverlay()
        media.show(tab.id, source, view, callback)
    }
    fun hideCustomView(source: WebView? = null) { if (source == null || media.sourceWebView === source) media.hide() }
    fun videoLoadingView(): View = MediaOverlay.loadingView(activity)
    fun sslError(view: WebView, handler: SslErrorHandler, error: SslError) = ssl.handle(view, handler, error)
    fun javascriptDialog(view: WebView, kind: moe.via.browser.browser.page.dialog.JavaScriptDialogKind, url: String, message: String?, defaultValue: String?, result: android.webkit.JsResult) =
        webDialogs.show(view, kind, url, message, defaultValue, result)
    fun formResubmission(view: WebView, cancel: android.os.Message, resend: android.os.Message) = webDialogs.resubmit(view, cancel, resend)
    fun key(event: KeyEvent): Boolean = input.onKey(event)
    fun back(): Boolean = media.onBack()
    fun resume() {
        host.chrome().refreshViewportPolicy()
        if (host.inputState().customTab || settings.preferences.autoHide == 0 && !settings.fullscreen) host.chrome().setChromeVisible(true)
        host.current()?.resume(); media.onResume(); updateChromeControls()
    }
    private fun changeChromeVisibility(visible: Boolean) {
        host.chrome().setChromeVisible(visible || host.inputState().customTab)
        updateChromeControls()
    }
    fun updateChromeControls() {
        val state = host.inputState()
        val show = windowFocused && !state.customTab && settings.preferences.autoHide == 2 && !media.active && !state.gameMode &&
            !state.keyboardVisible && !state.inputEditing && !state.contentOverlayOpen && !host.chrome().isChromeVisible
        if (show) reveal.show() else reveal.hide()
    }
    fun pause() {
        reveal.hide(false)
        input.reset(); media.onPause()
        if (!media.keepWebViewResumed) host.current()?.pause()
    }
    fun pictureInPictureChanged(inPip: Boolean) = media.onPictureInPictureModeChanged(inPip)
    fun configurationChanged(configuration: Configuration) {
        host.chrome().refreshViewportPolicy()
        media.onConfigurationChanged(configuration); reveal.updatePosition(); updateChromeControls()
    }
    fun windowFocusChanged(focused: Boolean) {
        windowFocused = focused
        media.onWindowFocusChanged(focused)
        updateChromeControls()
    }
    fun dispose() { reveal.dispose(); input.dispose(); media.dispose(); ssl.dispose(); webDialogs.dispose() }
}
