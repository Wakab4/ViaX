package moe.via.browser.feature.useragent.desktop

/** GP i6.h.A and i6.i0.g/h deliberately operate on raw strings rather than decoded URI parameters. */
internal object DesktopQuery {
    fun remove(url: String, parameter: String): String? {
        if (url.isEmpty() || parameter.isEmpty()) return null
        val at = url.indexOf(parameter)
        if (at <= 0) return null
        val end = at + parameter.length
        return when (url[at - 1]) {
            '&' -> if (url.length < end + 1 || url[end] == '&') url.substring(0, at - 1) + url.substring(end) else null
            '?' -> when {
                url.length > end && url[end] == '&' -> url.substring(0, at) + url.substring(end + 1)
                url.length == end -> url.substring(0, at)
                else -> null
            }
            else -> null
        }
    }

    fun value(url: String, name: String): String? {
        if (url.isEmpty() || name.isEmpty() || name.length >= url.length) return null
        var start = 0
        while (true) {
            val at = url.indexOf(name, start)
            if (at < 0) return null
            start = at + name.length
            if (at > 0 && (url[at - 1] == '&' || url[at - 1] == '?')) break
        }
        var end = url.indexOf('&', start)
        val hash = url.indexOf('#', start)
        if (hash > 0 && (hash < end || end < 0)) end = hash
        if (end < 0) end = url.length
        return url.substring(start, end)
    }
}
