package moe.via.browser.browser.page.lifecycle

import android.content.Context
import android.webkit.WebView
import moe.via.browser.feature.adblock.AdblockPagePolicy
import moe.via.browser.feature.passwords.web.PasswordPageScripts
import moe.via.browser.storage.preferences.BrowserPreferences
import moe.via.browser.storage.preferences.BrowserNightMode
import moe.via.browser.webview.bridge.NativePageBridge
import moe.via.browser.webview.clipboard.ClipboardPageScript
import moe.via.browser.webview.compat.NativeHeadScripts
import moe.via.browser.webview.compat.NativePageAddress
import moe.via.browser.webview.compat.WebDarkMode
import moe.via.browser.feature.fonts.web.NativePageFont
import moe.via.browser.webview.print.PagePrintPolicy

/** s6.va(1) evaluates the combined built-in head script before user scripts. */
internal class NativeHeadPolicy(context: Context, private val preferences: BrowserPreferences,
    private val clipboard: (String) -> Int, private val desktop: (String) -> Boolean,
    private val font: NativePageFont) {
    private val scripts = NativeHeadScripts(context)

    fun inject(view: WebView, url: String) {
        val source = buildString {
            append(AdblockPagePolicy.head(view, url))
            append(scripts.viewport(NativePageAddress.authority(url), desktop(url), preferences.forceZoom))
            append(ClipboardPageScript.create(view.context, clipboard(url)))
            append(WebDarkMode.headScript(view, BrowserNightMode.global(view.context), preferences.forceDark))
            append(NativePageBridge.headScript(view))
            append(PagePrintPolicy.script(view.context))
            append(scripts.notification)
            if (!preferences.vibrationAllowed) append(scripts.vibration)
            append(PasswordPageScripts.captureScript(view.context, NativePageBridge.secret(view)))
            append(font.script(preferences.uiFont))
        }
        if (source.isNotEmpty()) view.evaluateJavascript(source, null)
    }
}
