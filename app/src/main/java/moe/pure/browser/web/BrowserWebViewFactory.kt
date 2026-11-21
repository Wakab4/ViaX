package moe.pure.browser.web

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Build
import android.os.Message
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceResponse
import moe.pure.browser.feature.home.HomeDocument
import moe.pure.browser.feature.network.NetworkLogPages
import moe.pure.browser.web.bridge.NativePageBridge
import moe.pure.browser.web.bridge.NativePageDownload
import moe.pure.browser.web.bridge.NativePageMessage
import moe.pure.browser.web.lifecycle.NativePageLifecycle
import moe.pure.browser.web.permissions.WebPermissionsController
import moe.pure.browser.model.BrowserTab

class BrowserWebViewFactory(
    private val preferences: SharedPreferences,
    private val callbacks: Callbacks,
    private val permissions: WebPermissionsController,
    private val blockRequest: (BrowserTab, WebView, WebResourceRequest, String) -> Boolean = { _, _, _, _ -> false },
    private val contextMenu: (BrowserTab, WebView) -> Boolean = { _, _ -> false }
) {
    private val pageSecret = java.util.UUID.randomUUID().toString()
    private val pageLifecycle = NativePageLifecycle(preferences, callbacks::clipboardDecision, callbacks::desktopDecision, callbacks::readerProbe)
    fun pagePhase(view: WebView, runAt: Int) = pageLifecycle.phase(view, runAt)
    fun pageSettingsChanged(view: WebView) = pageLifecycle.settingsChanged(view)
    interface Callbacks {
        fun interceptNavigation(view: WebView, url: String): Boolean = false
        fun clipboardDecision(url: String): Int = moe.pure.browser.feature.site.SiteDecision.ALLOW
        fun desktopDecision(url: String): Boolean
        fun readerProbe()
        fun pageCommand(value: Int)
        fun pageControlsVisible(): Boolean
        fun pageAddon(payload: String)
        fun pageMessage(value: NativePageMessage)
        fun preparePage(tab: BrowserTab, source: WebView, targetUrl: String) = Unit
        fun shouldOpenExternally(tab: BrowserTab, source: WebView, url: String): Boolean
        fun pageStarted(tab: BrowserTab, source: WebView, url: String, rawUrl: String)
        fun pageFinished(tab: BrowserTab, source: WebView, url: String)
        fun titleChanged(tab: BrowserTab, source: WebView, title: String?)
        fun urlChanged(tab: BrowserTab, source: WebView)
        fun iconChanged(tab: BrowserTab, source: WebView)
        fun progressChanged(tab: BrowserTab, source: WebView, progress: Int)
        fun pageError(tab: BrowserTab, view: WebView, code: Int, description: String?, url: String?): Boolean
        fun createPopup(tab: BrowserTab, source: WebView): WebView?
        fun chooseFile(tab: BrowserTab, source: WebView, callback: ValueCallback<Array<android.net.Uri>>, params: WebChromeClient.FileChooserParams): Boolean
        fun download(tab: BrowserTab, source: WebView, url: String, userAgent: String?, disposition: String?, mimeType: String?, length: Long)
        fun pageDownload(tab: BrowserTab, source: WebView, value: NativePageDownload)
        fun navigateHome(tab: BrowserTab, source: WebView, address: String) = source.loadUrl(address)
        fun scriptOpen(url: String, active: Boolean, insert: Int) = Unit
        fun scriptDownload(url: String, name: String) = Unit
        fun selectedText(tab: BrowserTab, source: WebView, text: String, find: Boolean) = Unit
        fun httpAuthentication(tab: BrowserTab, view: WebView, handler: android.webkit.HttpAuthHandler, host: String) { handler.cancel() }
        fun showCustomView(tab: BrowserTab, source: WebView, custom: android.view.View, callback: WebChromeClient.CustomViewCallback) { callback.onCustomViewHidden() }
        fun hideCustomView(tab: BrowserTab, source: WebView) = Unit
        fun videoLoadingView(): android.view.View? = null
        fun sslError(view: WebView, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError) { handler.cancel() }
        fun pageTouch(tab: BrowserTab, view: WebView, event: android.view.MotionEvent): Boolean = false
        fun pageTouchDown(tab: BrowserTab, source: WebView, hasUrl: Boolean) = Unit
        fun pageOverScrolled(tab: BrowserTab, source: WebView, clampedX: Boolean) = Unit
        fun tabReleased(tab: BrowserTab, view: WebView) = Unit
        fun javascriptDialog(view: WebView, kind: moe.pure.browser.web.dialog.JavaScriptDialogKind, url: String, message: String?, defaultValue: String?, result: android.webkit.JsResult): Boolean = false
        fun formResubmission(view: WebView, cancel: Message, resend: Message) { cancel.sendToTarget() }
        fun closeWindow(tab: BrowserTab, source: WebView) = Unit
    }

    fun configure(tab: BrowserTab, view: WebView) {
        pageLifecycle.settingsChanged(view)
        NativeWebViewDefaults.apply(view)
        @Suppress("DEPRECATION")
        view.settings.apply {
            javaScriptEnabled = moe.pure.browser.feature.settings.data.BrowserPreferences(preferences).javascript
            setSupportMultipleWindows(true)
            if (Build.VERSION.SDK_INT >= 26) safeBrowsingEnabled = true
        }
        view.isFocusableInTouchMode = true
        (view as? moe.pure.browser.web.selection.BrowserPageView)?.selectionActions = object : moe.pure.browser.web.selection.BrowserPageView.SelectionActions {
            override fun search(text: String) { if (tab.isCurrent(view)) callbacks.selectedText(tab, view, text, false) }
            override fun find(text: String) { if (tab.isCurrent(view)) callbacks.selectedText(tab, view, text, true) }
        }
        (view as? moe.pure.browser.web.selection.BrowserPageView)?.interaction = object : moe.pure.browser.web.selection.BrowserPageView.Interaction {
            override fun touch(source: WebView, event: android.view.MotionEvent) = tab.isCurrent(source) && callbacks.pageTouch(tab, source, event)
            override fun down(hasUrl: Boolean) { if (tab.isCurrent(view)) callbacks.pageTouchDown(tab, view, hasUrl) }
            override fun overScrolled(clampedX: Boolean) { if (tab.isCurrent(view)) callbacks.pageOverScrolled(tab, view, clampedX) }
            override fun released(source: WebView) = callbacks.tabReleased(tab, source)
        }
        NativePageBridge.install(view, pageSecret, object : NativePageBridge.Actions {
            override fun command(value: Int) = callbacks.pageCommand(value)
            override fun controlsVisible() = callbacks.pageControlsVisible()
            override fun installedAddonIds() = moe.pure.browser.feature.scripts.runtime.NativeScriptPageApi.installedAddonIds(view.context)
            override fun addon(value: String) = callbacks.pageAddon(value)
            override fun record(url: String, selector: String?) {
                val app = view.context.applicationContext
                moe.pure.browser.data.BrowserExecutors.persistence.execute {
                    moe.pure.browser.feature.adblock.CustomBlockRules(app).addElement(url, selector.orEmpty())
                }
            }
            override fun toast(value: String) = moe.pure.browser.ui.message.BrowserMessages.show(view.context, value)
            override fun message(value: NativePageMessage) = callbacks.pageMessage(value)
        }) { callbacks.pageDownload(tab, view, it) }
        moe.pure.browser.feature.translation.PageTranslationBridge.install(view)
        moe.pure.browser.feature.scripts.runtime.UserScriptBridge.install(view, object : moe.pure.browser.feature.scripts.runtime.ScriptActions {
            override fun open(url: String, active: Boolean, insert: Int) = callbacks.scriptOpen(url, active, insert)
            override fun download(url: String, name: String) = callbacks.scriptDownload(url, name)
        })
        view.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!tab.owns(view)) return true
                if (callbacks.interceptNavigation(view, request.url.toString())) return true
                tab.beforeNavigation(view, request.url.toString())?.let { return it }
                if (tab.sourceUrl(view)?.startsWith("view-source:", true) == true && request.isForMainFrame && request.hasGesture() && !request.url.toString().startsWith("view-source:", true)) {
                    tab.urlChanged(view, request.url.toString())
                }
                if (moe.pure.browser.feature.adblock.AdblockNavigationState.isContinue(request.url)) {
                    moe.pure.browser.feature.adblock.AdblockNavigationState.target(request.url)?.let { target ->
                        moe.pure.browser.feature.adblock.AdblockNavigationState.of(tab).allow(target)
                        view.post { if (tab.owns(view)) callbacks.navigateHome(tab, view, target) }
                    }
                    return true
                }
                if (callbacks.shouldOpenExternally(tab, view, request.url.toString())) return true
                // p4.j derives the existing r4 decision from redirect/no-gesture, without a main-frame gate.
                return tab.shouldOverride(view, request.url.toString(), request.isRedirect || !request.hasGesture())
            }

            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (!tab.owns(view)) return null
                if (pageLifecycle.fontMatches(view, request.url.toString())) return pageLifecycle.fontResponse(view)
                val pageUrl = tab.sourceUrl(view).orEmpty()
                if (HomeDocument.isHome(view.context, pageUrl) || request.isForMainFrame && HomeDocument.isHome(view.context, request.url.toString())) {
                    HomeDocument.response(view.context, request.url, moe.pure.browser.ui.theme.BrowserNightMode.global(view.context))?.let { return it }
                }
                NetworkLogPages.response(view.context, request.url, moe.pure.browser.ui.theme.BrowserNightMode.global(view.context))?.let { return it }
                moe.pure.browser.feature.adblock.AdblockPagePolicy.response(view, request.url.toString())?.let { return it }
                val blocked = blockRequest(tab, view, request, pageUrl)
                tab.pageId(view)?.let { pageId ->
                    tab.requests.record(pageId, request.url.toString(), blocked, range = request.requestHeaders.entries.firstOrNull { it.key.equals("Range", true) }?.value)
                }
                return if (blocked) moe.pure.browser.feature.adblock.AdblockResponses.response(view.context, tab, request.url.toString()) else null
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (!tab.owns(view)) return
                (view as? moe.pure.browser.web.selection.BrowserPageView)?.cancelSelection()
                val logical = callbackUrl(tab, view, url)
                tab.pageId(view)?.let(tab.requests::pageStarted)
                moe.pure.browser.feature.translation.PageTranslationBridge.navigation(view)
                tab.pageStarted(view, logical)
                callbacks.pageStarted(tab, view, logical, url)
                pageLifecycle.started(view, url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                if (!tab.owns(view)) return
                val logical = callbackUrl(tab, view, url)
                tab.pageFinished(view, logical)
                pageLifecycle.finished(view, url)
                callbacks.pageFinished(tab, view, logical)
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                if (!tab.owns(view)) return
                // A same-document navigation updates the address without invalidating its document bridges.
                tab.urlChanged(view, callbackUrl(tab, view, url))
                callbacks.urlChanged(tab, view)
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (tab.owns(view) && request.isForMainFrame) callbacks.pageError(tab, view, error.errorCode, error.description?.toString(), request.url?.toString())
            }
            override fun onReceivedHttpAuthRequest(view: WebView, handler: android.webkit.HttpAuthHandler, host: String, realm: String?) {
                if (tab.owns(view)) callbacks.httpAuthentication(tab, view, handler, host) else handler.cancel()
            }
            override fun onReceivedSslError(view: WebView, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError) {
                if (tab.owns(view)) callbacks.sslError(view, handler, error) else handler.cancel()
            }
            override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
                if (tab.owns(view)) callbacks.formResubmission(view, dontResend, resend) else dontResend.sendToTarget()
            }

        }
        view.webChromeClient = object : WebChromeClient() {
            override fun getDefaultVideoPoster(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            override fun onCloseWindow(window: WebView) { if (tab.owns(window)) callbacks.closeWindow(tab, window) }
            override fun onJsAlert(view: WebView, url: String, message: String?, result: android.webkit.JsResult) =
                javascriptDialog(tab, view, moe.pure.browser.web.dialog.JavaScriptDialogKind.ALERT, url, message, null, result)
            override fun onJsConfirm(view: WebView, url: String, message: String?, result: android.webkit.JsResult) =
                javascriptDialog(tab, view, moe.pure.browser.web.dialog.JavaScriptDialogKind.CONFIRM, url, message, null, result)
            override fun onJsPrompt(view: WebView, url: String, message: String?, defaultValue: String?, result: android.webkit.JsPromptResult) =
                javascriptDialog(tab, view, moe.pure.browser.web.dialog.JavaScriptDialogKind.PROMPT, url, message, defaultValue, result)
            override fun onJsBeforeUnload(view: WebView, url: String, message: String?, result: android.webkit.JsResult) =
                javascriptDialog(tab, view, moe.pure.browser.web.dialog.JavaScriptDialogKind.BEFORE_UNLOAD, url, message, null, result)
            override fun onShowCustomView(custom: android.view.View, callback: CustomViewCallback) {
                if (tab.owns(view)) callbacks.showCustomView(tab, view, custom, callback) else callback.onCustomViewHidden()
            }
            @Deprecated("Legacy WebView fullscreen callback")
            override fun onShowCustomView(custom: android.view.View, requestedOrientation: Int, callback: CustomViewCallback) = onShowCustomView(custom, callback)
            override fun onHideCustomView() { if (tab.owns(view)) callbacks.hideCustomView(tab, view) }
            override fun getVideoLoadingProgressView(): android.view.View? = if (tab.owns(view)) callbacks.videoLoadingView() else null
            override fun onPermissionRequest(request: android.webkit.PermissionRequest) { if (tab.owns(view)) permissions.media(request) else request.deny() }
            override fun onPermissionRequestCanceled(request: android.webkit.PermissionRequest) = permissions.cancel(request)
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: android.webkit.GeolocationPermissions.Callback) {
                if (tab.owns(view)) permissions.geolocation(origin, callback) else callback.invoke(origin, false, false)
            }
            override fun onGeolocationPermissionsHidePrompt() { if (tab.owns(view)) permissions.cancelGeolocation() }
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (!tab.owns(view)) return
                tab.progressChanged(view, newProgress)
                callbacks.progressChanged(tab, view, newProgress)
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                if (!tab.owns(view)) return
                tab.titleChanged(view, title)
                callbacks.titleChanged(tab, view, title)
            }
            override fun onReceivedIcon(view: WebView, icon: Bitmap?) {
                if (!tab.owns(view)) return
                moe.pure.browser.feature.library.data.BookmarkFaviconStore.save(view.context, tab.sourceUrl(view).orEmpty(), icon)
                callbacks.iconChanged(tab, view)
            }

            override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
                if (!tab.owns(view)) return
                val pageUrl = tab.sourceUrl(view).orEmpty()
                if (!HomeDocument.isHome(view.context, pageUrl) && pageUrl.startsWith("http")) moe.pure.browser.feature.home.HomeIconFetcher.touch(view.context, pageUrl, url)
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<android.net.Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                if (!tab.owns(webView)) { filePathCallback.onReceiveValue(null); return true }
                val delivered = java.util.concurrent.atomic.AtomicBoolean(false)
                val result = ValueCallback<Array<android.net.Uri>> { value ->
                    if (delivered.compareAndSet(false, true)) filePathCallback.onReceiveValue(if (tab.owns(webView)) value else null)
                }
                return callbacks.chooseFile(tab, webView, result, fileChooserParams)
            }

            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message
            ): Boolean {
                if (!tab.owns(view)) return false
                // e8.c0.e acknowledges hidden-page popup requests without creating a window.
                if (!view.isShown) return true
                permissions.popup(tab.sourceUrl(view).orEmpty(), isUserGesture) { allowed ->
                    (resultMsg.obj as WebView.WebViewTransport).webView = if (allowed && tab.owns(view)) callbacks.createPopup(tab, view) else null
                    resultMsg.sendToTarget()
                }
                return true
            }
        }
        view.setDownloadListener { url, userAgent, disposition, mimeType, length ->
            if (tab.owns(view)) callbacks.download(tab, view, url, userAgent, disposition, mimeType, length)
        }
        view.setOnLongClickListener { tab.isCurrent(view) && contextMenu(tab, view) }
        WebTouchPoint.observe(view)
    }

    private fun javascriptDialog(tab: BrowserTab, source: WebView, kind: moe.pure.browser.web.dialog.JavaScriptDialogKind,
        url: String, message: String?, defaultValue: String?, result: android.webkit.JsResult): Boolean {
        if (!tab.owns(source)) {
            if (kind == moe.pure.browser.web.dialog.JavaScriptDialogKind.BEFORE_UNLOAD) result.confirm() else result.cancel()
            return true
        }
        return callbacks.javascriptDialog(source, kind, url, message, defaultValue, result)
    }

    private fun callbackUrl(tab: BrowserTab, view: WebView, url: String): String {
        if (tab.sourceUrl(view)?.startsWith("view-source:") == true && !url.startsWith("view-source:")) return "view-source:$url"
        return moe.pure.browser.web.error.BrowserErrorController.logicalUrl(tab.normalizeAddress(url))
    }
}
