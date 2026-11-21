package moe.pure.browser.feature.adblock

import android.content.Context
import android.util.Base64
import android.webkit.WebResourceResponse
import moe.pure.browser.model.BrowserTab
import moe.pure.browser.ui.theme.palette
import java.net.URLEncoder

object AdblockResponses {
    private val gif = Base64.decode("R0lGODlhAQABAID/AMDAwAAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==", Base64.DEFAULT)
    @Volatile private var template: String? = null
    private val placeholder = Regex("\\{\\{(\\w+)\\}\\}")
    fun response(context: Context, tab: BrowserTab, url: String): WebResourceResponse? {
        if (url == tab.url) {
            if (AdblockNavigationState.of(tab).allowed(url)) return null
            val rule = AdblockEngine.get(context).documentRule(url)
            return WebResourceResponse("text/html", "UTF-8", document(context, url, rule).byteInputStream()).apply {
                responseHeaders = mapOf("Cache-Control" to "no-cache", "Access-Control-Allow-Origin" to "*", "Content-Type" to "text/css")
            }
        }
        // The reference checks its dot-prefixed extension against this literal list.
        val extension = extension(url)
        return if (extension.length > 1 && "html|htm|css|js".contains(extension)) WebResourceResponse("text/plain", "UTF-8", byteArrayOf().inputStream())
            else WebResourceResponse("image/gif", "UTF-8", gif.inputStream())
    }
    fun document(context: Context, url: String, rule: String): String {
        val html = template ?: context.assets.open("adblock/blocked.html").bufferedReader().use { it.readText().trimEnd() }.also { template = it }
        val night = context.palette.night
        val values = mapOf("title" to escape(AdblockNavigationState.host(url)), "background" to if (night) "#000" else "#fff",
            "text" to if (night) "#fff" else "#000", "code" to if (night) "#303030" else "#e0e0e6",
            "url" to escape(url), "rule" to escape(rule), "continue" to escape("v://blocker/jump?url=" + URLEncoder.encode(url, "UTF-8")))
        return placeholder.replace(html) { match -> values.getValue(match.groupValues[1]) }
    }
    private fun extension(url: String): String {
        var value = url.substringBeforeLast('?', url).substringAfterLast('/')
        value.lastIndexOf('.').takeIf { it >= 0 }?.let { value = value.substring(it) }
        return value.takeIf { it.length in 2..6 && it.startsWith('.') }.orEmpty()
    }
    private fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
}
