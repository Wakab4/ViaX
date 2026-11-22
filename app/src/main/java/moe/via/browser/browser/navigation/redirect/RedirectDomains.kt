package moe.via.browser.browser.navigation.redirect

import java.util.Locale

/** GP i6.i0.s/f/b: literal string rules, not a URI parser or a public-suffix list. */
internal object RedirectDomains {
    fun network(url: String): Boolean = url.length > 6 &&
        (url.startsWith("http://", true) || url.length > 7 && url.startsWith("https://", true))

    fun base(url: String): String {
        val scheme = url.indexOf("://")
        if (scheme < 0) return ""
        val start = scheme + 3
        val host = url.substring(start, url.indexOf('/', start).takeIf { it >= 0 } ?: url.length).substringBefore(':')
        if (host.isEmpty() || host.count { it == '.' } == 3 && host.all { it == '.' || it in '0'..'9' }) return host
        val last = host.lastIndexOf('.')
        if (last < 0) return host
        val previous = host.lastIndexOf('.', last - 1)
        if (previous < 0) return host
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
}
