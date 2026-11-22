package moe.via.browser.webview.bridge

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import moe.via.browser.R
import moe.via.browser.webview.bridge.download.NativeDownloadNames
import moe.via.browser.webview.bridge.download.NativePageDownloadScript
import org.json.JSONObject

data class NativePageDownload(val url: String?, val data: String?, val name: String?)

/** Native via facade; feature work is delegated to its existing owners. */
internal object NativePageBridge {
    interface Actions {
        fun command(value: Int)
        fun controlsVisible(): Boolean
        fun installedAddonIds(): String
        fun addon(value: String)
        fun record(url: String, selector: String?)
        fun toast(value: String)
        fun message(value: NativePageMessage)
    }
    fun install(view: WebView, secret: String, actions: Actions, downloaded: (NativePageDownload) -> Unit) {
        val session = Session(secret, actions, downloaded)
        view.setTag(R.id.native_page_bridge, session)
        view.addJavascriptInterface(session.Receiver(), "via")
    }
    fun headScript(view: WebView) = NativePageDownloadScript.hooks(secret(view))
    fun image(view: WebView, url: String) = evaluate(view) { NativePageDownloadScript.image(it, url) }
    fun blob(view: WebView, url: String) = evaluate(view) { NativePageDownloadScript.blob(it, url) }
    fun stream(view: WebView, url: String) = evaluate(view) { NativePageDownloadScript.stream(it, url) }
    internal fun secret(view: WebView): String = (view.getTag(R.id.native_page_bridge) as Session).secret
    private fun evaluate(view: WebView, script: (String) -> String) {
        val session = view.getTag(R.id.native_page_bridge) as Session
        view.evaluateJavascript(script(session.secret), null)
    }
    fun dispose(view: WebView) {
        (view.getTag(R.id.native_page_bridge) as? Session)?.closed = true
        view.removeJavascriptInterface("via"); view.setTag(R.id.native_page_bridge, null)
    }
    private class Session(val secret: String, private val actions: Actions, private val downloaded: (NativePageDownload) -> Unit) {
        private val main = Handler(Looper.getMainLooper())
        @Volatile var closed = false
        inner class Receiver {
            @JavascriptInterface fun addon(value: String?) {
                if (!closed && !value.isNullOrEmpty()) main.post { if (!closed) actions.addon(value) }
            }
            @JavascriptInterface fun cmd(value: Int): Int {
                if (closed) return 0
                if (value == 515) return if (actions.controlsVisible()) 1 else 0
                if (value in setOf(257, 514, 516, 517)) main.post { if (!closed) actions.command(value) }
                return 0
            }
            @JavascriptInterface fun getInstalledAddonID(): String = if (closed) "[]" else actions.installedAddonIds()
            @JavascriptInterface fun record(url: String?, selector: String?) {
                if (!closed && !url.isNullOrEmpty() && !url.startsWith("file://")) actions.record(url, selector)
            }
            @JavascriptInterface fun toast(value: String?) {
                if (!closed && value != null) main.post { if (!closed) actions.toast(value) }
            }
            @JavascriptInterface fun download(token: String?, url: String?, data: String?) {
                if (closed || token.isNullOrEmpty() || token != secret) return
                val name = NativeDownloadNames.find(url)
                main.post { if (!closed) downloaded(NativePageDownload(url, data, name)) }
            }
            @JavascriptInterface fun postMessage(token: String?, message: String?) {
                if (closed || token.isNullOrEmpty() || token != secret || message == null || message.length < 2) return
                try {
                    val value = JSONObject(message)
                    if (value.optInt("action") == 104) NativeDownloadNames.remember(value.optString("url"), value.optString("name"))
                    else NativePageMessage.read(value)?.let { event -> main.post { if (!closed) actions.message(event) } }
                } catch (_: org.json.JSONException) { }
            }
        }
    }
}
