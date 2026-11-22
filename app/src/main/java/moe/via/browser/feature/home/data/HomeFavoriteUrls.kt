package moe.via.browser.feature.home.data

import moe.via.browser.feature.search.resolve.SearchTemplate
import moe.via.browser.feature.search.resolve.SearchUrlResolver
import java.net.IDN
import java.net.URLDecoder

/** Native z8.i0.f variants; queries and schemes are not collapsed into a different site. */
object HomeFavoriteUrls {
    fun equivalents(seed: String): List<String> {
        val url = seed.trim { it <= ' ' }
        if (url.isEmpty()) return emptyList()
        val result = arrayListOf(url)
        val scheme = url.indexOf("://")
        val slash = if (scheme > 0) url.indexOf('/', scheme + 3) else -1
        val rootSlash = scheme > 0 && url.endsWith('/') && slash == url.lastIndex
        if (rootSlash) result.add(url.dropLast(1))
        var unicode: String? = null
        if (scheme > 0 && url.indexOf("xn--", scheme + 3) > 0) {
            val hostStart = scheme + 3
            val hostEnd = slash.takeIf { it >= 0 } ?: url.length
            unicode = url.substring(0, hostStart) + runCatching { IDN.toUnicode(url.substring(hostStart, hostEnd)) }
                .getOrDefault(url.substring(hostStart, hostEnd)) + url.substring(hostEnd)
            result.add(unicode)
            if (rootSlash) result.add(unicode.dropLast(1))
        }
        if (slash > 0 && !rootSlash && '%' in url.substring(slash)) {
            val path = runCatching { URLDecoder.decode(url.substring(slash), "UTF-8") }.getOrDefault(url.substring(slash))
            result.add(url.substring(0, slash) + path)
            unicode?.let { result.add(it.substring(0, it.indexOf('/', scheme + 3)) + path) }
        }
        return result.toList()
    }

    fun href(url: String): String = if (SearchUrlResolver.isHttp(url) || url.startsWith("file://", true) || url.startsWith("data:", true))
        "v://error/jump?url=" + SearchTemplate.encode(url) else url
}
