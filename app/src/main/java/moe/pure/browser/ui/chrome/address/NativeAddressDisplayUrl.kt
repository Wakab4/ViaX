package moe.pure.browser.ui.chrome.address

import android.webkit.URLUtil
import moe.pure.browser.core.NativeUrlDecoder
import moe.pure.browser.web.compat.NativePageAddress
import java.net.URLDecoder

/** z8.w2.f and z8.b0.E/i6.g0.f are display transforms, never navigation normalization. */
object NativeAddressDisplayUrl {
    private const val ERROR_PREFIX = "v://error/jump?url="

    fun unwrap(value: String?): String? {
        if (value == null || !value.startsWith(ERROR_PREFIX, ignoreCase = true)) return value
        val end = value.indexOf('&', ERROR_PREFIX.length).takeIf { it >= 0 } ?: value.length
        val encoded = value.substring(ERROR_PREFIX.length, end)
        return runCatching { URLDecoder.decode(encoded, "UTF-8") }.getOrDefault(encoded)
    }

    internal fun display(value: String): String = decode(value).take(8192)

    internal fun url(value: String): String {
        if (!URLUtil.isNetworkUrl(value)) return value
        val stripped = value.substring(value.indexOf("://") + 3)
        val slash = stripped.indexOf('/')
        return if (slash == stripped.length - 1) stripped.substring(0, slash) else stripped
    }

    internal fun domain(value: String): String = NativePageAddress.authority(value).ifEmpty { value }

    private fun decode(value: String): String {
        if (value.length < 8) return value
        val separator = value.indexOf("://")
        if (separator < 4) return value
        if (separator >= 5 && !value.regionMatches(separator - 5, "https", 0, 5, ignoreCase = true) &&
            !value.regionMatches(separator - 4, "http", 0, 4, ignoreCase = true)) return value
        val start = separator + 3
        val slash = value.indexOf('/', start)
        val candidate = value.indexOf("xn--", start)
        // DEX @26d638: a positive candidate is discarded when slash == -1 too.
        val punycode = if (candidate > 0 && candidate >= slash) -1 else candidate
        if (slash < 0) return if (punycode < 0) value
            else value.substring(0, start) + NativeAddressPunycode.decode(value.substring(start))
        val percent = value.indexOf('%', slash + 1)
        if (punycode < 0 && percent < 0) return value
        return buildString {
            if (punycode > 0) {
                append(value.substring(0, start))
                append(NativeAddressPunycode.decode(value.substring(start, slash)))
            } else append(value.substring(0, slash))
            if (percent > 0) {
                append(value.substring(slash, percent))
                append(NativeUrlDecoder.guarded(value.substring(percent)))
            } else append(value.substring(slash))
        }
    }
}
