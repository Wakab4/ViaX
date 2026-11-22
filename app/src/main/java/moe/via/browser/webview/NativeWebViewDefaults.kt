package moe.via.browser.webview

import android.graphics.Color
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/** s4.b.f defaults shared by the browser and Native document viewers. */
internal object NativeWebViewDefaults {
    @Suppress("DEPRECATION")
    fun apply(view: WebView) {
        view.drawingCacheBackgroundColor = Color.TRANSPARENT
        view.isFocusable = true
        view.setBackgroundColor(Color.TRANSPARENT)
        view.isScrollbarFadingEnabled = true
        view.isSaveEnabled = true
        view.scrollBarSize = 10
        view.settings.apply {
            setEnableSmoothTransition(true)
            try { mediaPlaybackRequiresUserGesture = true } catch (_: Exception) { }
            domStorageEnabled = true
            setGeolocationDatabasePath(view.context.filesDir.toString())
            allowFileAccess = true
            databaseEnabled = true
            saveFormData = true
            setSupportZoom(true)
            javaScriptCanOpenWindowsAutomatically = true
            defaultTextEncodingName = "UTF-8"
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            allowContentAccess = true
            minimumFontSize = 1
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false
            try { mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW } catch (_: Exception) { }
        }
        CookieManager.getInstance().setAcceptCookie(true)
    }
}
