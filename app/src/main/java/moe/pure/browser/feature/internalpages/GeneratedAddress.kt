package moe.pure.browser.feature.internalpages

/** z8.w2/b0.Q use raw, case-aware substrings rather than a normalized URI host. */
internal object GeneratedAddress {
    fun normalize(url: String): String = if (url.startsWith("via://", ignoreCase = true)) "v://" + url.substring(6) else url
    fun isV(url: String) = url.startsWith("v://", ignoreCase = true)
    fun matches(url: String, prefix: String): Boolean = url.startsWith(prefix, ignoreCase = true) &&
        (url.length == prefix.length || url[prefix.length] == '?' || url[prefix.length] == '/')

    fun kind(url: String): Int {
        if (!isV(url)) return -1
        var end = url.indexOf('/', 4)
        if (end < 0) end = url.indexOf('?', 4)
        if (end < 0) end = url.length
        return when (url.substring(4, end)) {
            "offline" -> 7
            "log" -> 6
            "home" -> 1
            "about" -> 4
            "history" -> 3
            "bookmarks" -> 2
            else -> 5
        }
    }

    fun folder(url: String): String? {
        if (!matches(url, "v://bookmarks")) return null
        val marker = url.indexOf("?folder=")
        if (marker < 0) return null
        val start = marker + 8
        val end = url.indexOf('&', start).takeIf { it >= 0 } ?: url.length
        return url.substring(start, end)
    }
}
