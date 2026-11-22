package moe.via.browser.browser.page.lifecycle

import android.content.SharedPreferences
import android.webkit.WebView
import moe.via.browser.feature.internalpages.BrowserInternalPages
import moe.via.browser.feature.adblock.ElementPickerBridge
import moe.via.browser.feature.reader.ReaderPageBootstrap
import moe.via.browser.feature.scripts.runtime.UserScriptBridge
import moe.via.browser.storage.preferences.BrowserPreferences
import moe.via.browser.webview.bridge.NativePageBridge
import moe.via.browser.webview.compat.NativePageAddress
import moe.via.browser.webview.compat.NativeWebRtcPolicy
import moe.via.browser.feature.fonts.web.NativePageFont
import org.json.JSONObject
import moe.via.browser.webview.lifecycle.NativePagePhaseScript

/** e8.i0 and s6.va dispatch page phases; individual features keep their scripts and business state. */
internal class NativePageLifecycle(private val preferences: SharedPreferences, private val clipboardDecision: (String) -> Int,
    private val desktopDecision: (String) -> Boolean, private val probeCurrent: () -> Unit) {
    private val settings = BrowserPreferences(preferences)
    private var font: NativePageFont? = null
    private var head: NativeHeadPolicy? = null
    private var reader: ReaderPageBootstrap? = null

    private fun font(view: WebView) = font ?: NativePageFont(view.context).also { font = it }
    fun settingsChanged(view: WebView) = font(view).selected(settings.uiFont)
    fun fontMatches(view: WebView, url: String): Boolean = font(view).matches(url)
    fun fontResponse(view: WebView) = font(view).response(settings.uiFont)

    fun started(view: WebView, url: String) {
        NativeWebRtcPolicy.started(view, url, settings.disableWebRtc)
        if (generated(view, url)) { generatedSecret(view); return }
        val actual = view.url ?: return
        if (view.settings.javaScriptEnabled && actual.isNotEmpty() && !actual.startsWith("file://", true))
            view.evaluateJavascript(NativePagePhaseScript.create(NativePageBridge.secret(view), view.id), null)
    }

    fun finished(view: WebView, url: String) {
        if (generated(view, url)) generatedSecret(view)
    }

    private fun generated(view: WebView, url: String) = BrowserInternalPages.isInternal(view.context, url)
    private fun generatedSecret(view: WebView) {
        view.evaluateJavascript("(function(){window.__VIA_SECRET__=${JSONObject.quote(NativePageBridge.secret(view))}})()", null)
        font(view).script(settings.uiFont).takeIf(String::isNotEmpty)?.let { view.evaluateJavascript(it, null) }
    }

    fun phase(view: WebView, runAt: Int) {
        val url = view.url ?: return
        if (!view.settings.javaScriptEnabled || !NativePageAddress.acceptsPolicy(url)) return
        val phase = when (runAt) { 1 -> 1; 4 -> 4; else -> 2 }
        if (phase == 1) {
            val policy = head ?: NativeHeadPolicy(view.context, settings, clipboardDecision, desktopDecision, font(view)).also { head = it }
            policy.inject(view, url)
            UserScriptBridge.runAt(view, phase)
        } else if (phase == 4) {
            ElementPickerBridge.prepare(view)
            probeCurrent()
            UserScriptBridge.runAt(view, phase)
        } else {
            UserScriptBridge.runAt(view, phase)
            val bootstrap = reader ?: ReaderPageBootstrap(view.context).also { reader = it }
            bootstrap.prepare(view, probeCurrent)
        }
    }
}
