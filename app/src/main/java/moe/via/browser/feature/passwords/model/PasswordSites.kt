package moe.via.browser.feature.passwords.model

import moe.via.browser.feature.search.resolve.SearchPunycode
import java.util.Locale

/** GP i6.i0's string rules deliberately differ from Uri.host and a public suffix database. */
object PasswordSites {
    fun authority(url: String?): String {
        if (url == null) return ""
        val scheme = url.indexOf("://")
        if (scheme < 0) return ""
        val start = scheme + 3
        val end = url.indexOf('/', start).takeIf { it >= 0 } ?: url.length
        return url.substring(start, end)
    }

    fun baseDomain(url: String?): String {
        val host = authority(url).substringBefore(':')
        if (host.isEmpty() || host.startsWith('[') || host.contains(':') ||
            (host.count { it == '.' } == 3 && host.all { it == '.' || it in '0'..'9' })
        ) return host
        val last = host.lastIndexOf('.')
        val previous = host.lastIndexOf('.', last - 1)
        if (last < 0 || previous < 0) return host
        val suffix = host.substring(last + 1).lowercase(Locale.ROOT)
        val second = host.substring(previous + 1, last).lowercase(Locale.ROOT)
        if (suffix == "io" && second == "github") {
            val third = host.lastIndexOf('.', previous - 1)
            return if (third >= 0) host.substring(third + 1) else host
        }
        if (suffix in setOf("com", "net", "org", "gov", "co", "edu")) return host.substring(previous + 1)
        val third = host.lastIndexOf('.', previous - 1)
        return if (third >= 0) host.substring(third + 1) else host
    }

    fun normalizeEditor(input: String): String {
        val text = input.trim { it <= ' ' }.let { if (it.contains("://")) authority(it) else it }
        val end = text.indexOf('/').takeIf { it >= 0 } ?: text.indexOf(':').takeIf { it >= 0 } ?: text.length
        return SearchPunycode.host(text.substring(0, end)) + text.substring(end)
    }
    fun normalize(input: String) = normalizeEditor(input)
    fun isNetworkUrl(url: String?) = url != null &&
        ((url.length > 6 && url.startsWith("http://", true)) || (url.length > 7 && url.startsWith("https://", true)))

    /** Native k8.p groups rightmost labels first, keeping case and the original substring boundaries. */
    val siteOrder: Comparator<String> = Comparator { first, second ->
        var a = first.length; var b = second.length
        var left = a; var right = b
        var result = 0
        while (a > 0 && b > 0) {
            val dotA = first.lastIndexOf('.', a); val dotB = second.lastIndexOf('.', b)
            val startA = dotA + 1; val startB = dotB + 1
            val countA = a - startA; val countB = b - startB
            for (index in 0 until minOf(countA, countB)) {
                result = first[startA + index].code - second[startB + index].code
                if (result != 0) break
            }
            if (result != 0) break
            result = countA - countB
            if (result != 0) break
            a = dotA - 1; b = dotB - 1; left = startA; right = startB
        }
        if (result == 0) left - right else result
    }
}
