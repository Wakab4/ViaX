package moe.via.browser.browser.session.page

import android.view.ViewGroup
import android.webkit.WebView
import moe.via.browser.webview.selection.BrowserPageView

internal object PageViewLifecycle {
    fun pauseMedia(view: WebView) {
        view.evaluateJavascript("(function(){for(var c=document.querySelectorAll('video, audio'),a,b=0;b<c.length;b++)a=c[b],a.paused||(a.pause(),a.setAttribute('via-data-playing','true'))})();", null)
    }

    fun resumeMedia(view: WebView) {
        view.evaluateJavascript("(function(){for(var c=document.querySelectorAll('video[via-data-playing=\"true\"], audio[via-data-playing=\"true\"]'),a,b=0;b<c.length;b++)a=c[b],a.play(),a.removeAttribute('via-data-playing')})();", null)
    }

    fun destroy(view: BrowserPageView) {
        (view.parent as? ViewGroup)?.removeView(view)
        moe.via.browser.webview.bridge.NativePageBridge.dispose(view)
        moe.via.browser.feature.translation.PageTranslationBridge.dispose(view)
        view.disposeSelection()
        moe.via.browser.feature.scripts.runtime.UserScriptBridge.dispose(view)
        view.stopLoading()
        view.settings.javaScriptEnabled = false
        view.webViewClient = android.webkit.WebViewClient()
        view.webChromeClient = null
        view.clearHistory()
        view.clearCache(false)
        view.onPause()
        view.removeAllViews()
        view.destroyDrawingCache()
        view.destroy()
    }
}
