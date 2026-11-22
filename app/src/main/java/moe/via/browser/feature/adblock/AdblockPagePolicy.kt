package moe.via.browser.feature.adblock

import android.webkit.WebView
import android.webkit.WebResourceResponse
import moe.via.browser.webview.compat.NativePageAddress

object AdblockPagePolicy {
    private const val STYLESHEET = "via_inject_blocker.css"

    /** c8.s6.va(1) injects the inline append and the original synthetic stylesheet link. */
    fun head(view: WebView, url: String): String {
        if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) return ""
        val engine = AdblockEngine.get(view.context)
        if (!engine.enabled(url)) return ""
        val host = NativePageAddress.authority(url)
        inline(engine.css(host)).takeIf(String::isNotEmpty)?.let { view.evaluateJavascript(it, null) }
        return "(function(){if(!document.getElementById('via_inject_css_blocker')){var css=document.createElement('link');css.id='via_inject_css_blocker';css.type='text/css';css.rel=\"stylesheet\";css.href='https://$host/$STYLESHEET';var o=document.getElementsByTagName('head');if(o.length>0&&o[0].appendChild(css)){}}})();"
    }

    /** ua.E0 serves this URL before request blocking, without rechecking the page switch. */
    fun response(view: WebView, url: String): WebResourceResponse? {
        if (!url.endsWith(STYLESHEET)) return null
        val css = AdblockEngine.get(view.context).css(url)
        if (css.isEmpty()) return WebResourceResponse("text/plain", "UTF-8", byteArrayOf().inputStream())
        return WebResourceResponse("text/css", "UTF-8", css.byteInputStream()).apply {
            responseHeaders = mapOf("Cache-Control" to "no-cache", "Access-Control-Allow-Origin" to "*", "Content-Type" to "text/css")
        }
    }

    /** m8.b.a escapes quotes and numeric escapes only; retain the original character stream. */
    private fun inline(css: String): String {
        if (css.isEmpty()) return ""
        return buildString {
            append("javascript:(function(){function updateStyle(style){var css=document.getElementById('__via_blocker_css__');if(css){css.innerText+=style;return}css=document.createElement('style');css.type='text/css';css.charset='UTF-8';css.id='__via_blocker_css__';css.appendChild(document.createTextNode(style));document.head.appendChild(css)}updateStyle(\"")
            for (char in css) {
                if (char == '"') append('\\')
                else if (char in '0'..'9' && last() == '\\') append('\\')
                append(char)
            }
            append("\")})();")
        }
    }
}
