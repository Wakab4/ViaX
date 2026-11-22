package moe.via.browser.feature.adblock

import android.webkit.WebView
import moe.via.browser.webview.bridge.NativePageBridge
import org.json.JSONObject

/** Picker JavaScript shares the original root secret and via.postMessage 102/103. */
internal object ElementPickerBridge {
    fun start(view: WebView) {
        prepare(view)
        command(view, "window.__setMarkerEnabled(true)")
    }
    fun prepare(view: WebView) {
        val script = view.context.assets.open("adblock/element-picker.js").bufferedReader(Charsets.UTF_8).use { it.readText() }
            .replace("{{token}}", JSONObject.quote(NativePageBridge.secret(view)))
        view.evaluateJavascript(script, null)
    }
    fun command(view: WebView, code: String) = view.evaluateJavascript("try{$code}catch(ignored){}", null)
    fun stop(view: WebView) { command(view, "window.__setMarkerEnabled(false)") }
    fun hide(view: WebView, selector: String) {
        val rule = JSONObject.quote(selector + "{display:none !important}")
        view.evaluateJavascript("(function(){var s=document.getElementById('__via__marker_temp__');if(s)s.innerText+=$rule;else{s=document.createElement('style');s.type='text/css';s.charset='UTF-8';s.id='__via__marker_temp__';s.appendChild(document.createTextNode($rule));document.head.appendChild(s)}})()", null)
    }
}
