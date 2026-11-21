package moe.pure.browser.feature.translation

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import moe.pure.browser.R
import java.util.UUID

/** A single page translation request may ask the browser to offer CSP alternatives. */
class PageTranslationBridge private constructor(private val view: WebView) {
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var active: Request? = null
    private data class Request(val token: String, val url: String?, val fallback: () -> Unit)

    inner class Receiver {
        @JavascriptInterface fun fallback(token: String) {
            val request = active?.takeIf { it.token == token } ?: return
            main.post {
                if (active === request && view.url == request.url) {
                    active = null
                    request.fallback()
                }
            }
        }
    }

    private fun inject(script: (String) -> String, fallback: () -> Unit) {
        val request = Request(UUID.randomUUID().toString(), view.url, fallback)
        active = request
        view.evaluateJavascript(script(request.token), null)
    }

    companion object {
        fun install(view: WebView) {
            if (view.getTag(R.id.translation_bridge) != null) return
            val bridge = PageTranslationBridge(view)
            view.setTag(R.id.translation_bridge, bridge)
            view.addJavascriptInterface(bridge.Receiver(), "PureTranslation")
        }
        fun inject(view: WebView, script: (String) -> String, fallback: () -> Unit) {
            (view.getTag(R.id.translation_bridge) as? PageTranslationBridge)?.inject(script, fallback)
        }
        fun navigation(view: WebView) { (view.getTag(R.id.translation_bridge) as? PageTranslationBridge)?.active = null }
        fun dispose(view: WebView) {
            navigation(view)
            view.removeJavascriptInterface("PureTranslation")
            view.setTag(R.id.translation_bridge, null)
        }
    }
}
