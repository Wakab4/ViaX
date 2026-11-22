package moe.via.browser.webview.compat

import android.os.Build
import android.webkit.WebView
import android.view.View
import java.lang.ref.WeakReference
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/** e8.i.L and ua.n1 use separate engine and legacy-CSS strategies without navigating the page. */
object WebDarkMode {
    private var enter: String? = null
    private var exit: String? = null

    @Suppress("DEPRECATION")
    fun apply(view: WebView, night: Boolean, forceDark: Boolean) {
        if (Build.VERSION.SDK_INT < 29) return
        val enabled = night && forceDark
        when {
            WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING) ->
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(view.settings, enabled)
            WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) ->
                WebSettingsCompat.setForceDark(view.settings, if (enabled) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF)
        }
    }

    fun synchronizePage(view: WebView, url: String?, night: Boolean, forceDark: Boolean) {
        if (view.progress < 100 || url.isNullOrEmpty() || url.startsWith("file://", true)) return
        script(view, legacyEnabled(night, forceDark))
    }

    fun headScript(view: WebView, night: Boolean, forceDark: Boolean): String {
        if (!legacyEnabled(night, forceDark)) return ""
        val reference = WeakReference(view)
        view.postDelayed({ reference.get()?.visibility = View.VISIBLE }, 300)
        return enter ?: read(view, "night-enter.js").also { enter = it }
    }

    fun currentMask(night: Boolean, homeAppearance: Boolean, filter: Int): Int =
        if (night && !homeAppearance) filter.coerceIn(0, 255) else 0

    private fun legacyEnabled(night: Boolean, forceDark: Boolean) = night && forceDark && !WebViewCapabilities.darkeningSupported()

    private fun script(view: WebView, add: Boolean) {
        val source = if (add) enter ?: read(view, "night-enter.js").also { enter = it }
            else exit ?: read(view, "night-exit.js").also { exit = it }
        view.loadUrl("javascript:$source")
    }
    private fun read(view: WebView, name: String) = view.context.applicationContext.assets.open("theme/$name")
        .bufferedReader(Charsets.UTF_8).use { it.readText() }
}
