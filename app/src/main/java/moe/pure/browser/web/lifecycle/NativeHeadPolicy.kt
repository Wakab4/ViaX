package moe.pure.browser.web.lifecycle

import android.content.Context
import android.webkit.WebView
import moe.pure.browser.feature.adblock.AdblockPagePolicy
import moe.pure.browser.feature.passwords.web.PasswordPageScripts
import moe.pure.browser.feature.settings.data.BrowserPreferences
import moe.pure.browser.ui.theme.BrowserNightMode
import moe.pure.browser.web.bridge.NativePageBridge
import moe.pure.browser.web.clipboard.ClipboardPageScript
import moe.pure.browser.web.compat.NativeHeadScripts
import moe.pure.browser.web.compat.NativePageAddress
import moe.pure.browser.web.compat.WebDarkMode
import moe.pure.browser.web.font.NativePageFont
import moe.pure.browser.web.print.PagePrintPolicy

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
