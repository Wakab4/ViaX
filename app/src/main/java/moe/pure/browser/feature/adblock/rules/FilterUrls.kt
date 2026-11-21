package moe.pure.browser.feature.adblock.rules

/** URL classification used by the reference filter engine, separate from browser site settings. */
object FilterUrls {
    fun host(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        var value = url.substringAfter("://", url)
        value.indexOf('/').takeIf { it > 0 }?.let { value = value.substring(0, it) }
        value.lastIndexOf(':').takeIf { it > 0 }?.let { value = value.substring(0, it) }
        return value
    }
    fun baseDomain(host: String): String {
        val last = host.lastIndexOf('.')
        if (last < 0) return host
        var before = host.lastIndexOf('.', last - 1)
        if (before < 0) return host
        if (host.substring(before + 1, last) in setOf("com", "net", "org", "gov", "co")) before = host.lastIndexOf('.', before - 1)
        return host.substring(before + 1)
    }
    fun thirdParty(requestHost: String?, pageHost: String?): Boolean =
        requestHost != null && pageHost != null && requestHost != pageHost && !requestHost.contains(baseDomain(pageHost))
}
