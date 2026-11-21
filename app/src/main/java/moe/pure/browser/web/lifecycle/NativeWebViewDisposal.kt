package moe.pure.browser.web.lifecycle

import android.webkit.WebView
import android.webkit.WebViewClient

/** s4.b.a releases a WebView after its owner removes it from the view hierarchy. */
internal object NativeWebViewDisposal {
    @Suppress("DEPRECATION")
    fun destroy(view: WebView?) {
        if (view == null) return
        try {
            view.stopLoading()
            view.settings.javaScriptEnabled = false
            view.webViewClient = WebViewClient()
            view.webChromeClient = null
            view.tag = null
            view.clearHistory()
            view.clearCache(false)
            view.onPause()
            view.removeAllViews()
            view.destroyDrawingCache()
            view.destroy()
        } catch (_: Exception) {
        }
    }
}
