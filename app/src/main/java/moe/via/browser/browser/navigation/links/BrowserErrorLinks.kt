package moe.via.browser.browser.navigation.links

import java.net.URLDecoder
import java.net.URLEncoder

/** Native z8.w2.f/g/r and s6.u. These are internal bookmark links, not error-page HTML. */
object BrowserErrorLinks {
    private const val PREFIX = "v://error/jump?url="
    const val REDIRECT_ASSET_URL = "file:///android_asset/error.html"

    fun isJump(url: String): Boolean = normalize(url).startsWith(PREFIX, ignoreCase = true)

    fun target(url: String): String? {
        val normalized = normalize(url)
        if (!normalized.startsWith(PREFIX, ignoreCase = true)) return null
        val end = normalized.indexOf('&', PREFIX.length).takeIf { it >= 0 } ?: normalized.length
        val encoded = normalized.substring(PREFIX.length, end)
        return runCatching { URLDecoder.decode(encoded, Charsets.UTF_8.name()) }.getOrDefault(encoded)
    }

    fun logicalUrl(url: String): String = target(url) ?: url

    /** Only HTTP(S), file:// and data: targets get the Native bookmark indirection. */
    fun wrap(url: String): String {
        val supported = url.startsWith("http://", true) || url.startsWith("https://", true) ||
            url.startsWith("file://", true) || url.startsWith("data:", true)
        if (!supported) return url
        return PREFIX + URLEncoder.encode(url, Charsets.UTF_8.name()).replace("+", "%20")
    }

    internal fun canNavigate(target: String): Boolean {
        val schemeCheck = target.filterNot { it == '\t' || it == '\r' || it == '\n' }.trimStart()
        return schemeCheck.isNotEmpty() && !schemeCheck.startsWith("javascript:", ignoreCase = true)
    }

    private fun normalize(url: String) = if (url.startsWith("via://", true)) "v://" + url.substring(6) else url
}
