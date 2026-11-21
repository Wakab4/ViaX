package moe.pure.browser.feature.site

import moe.pure.browser.feature.search.resolve.SearchPunycode

/** Runtime identity is i6.i0.e's literal authority, not a normalized URI host. */
object NativeSiteKeys {
    fun fromUrl(url: String?): String {
        if (url == null) return ""
        val separator = url.indexOf("://")
        if (separator < 0) return ""
        val start = separator + 3
        val end = url.indexOf('/', start).takeIf { it >= 0 } ?: url.length
        return url.substring(start, end)
    }

    /** e8.i's popup destination deliberately uses i6.i0.f, including its first-colon behavior. */
    fun popupDestination(url: String?): String = fromUrl(url).substringBefore(':')

    /** hb.w6 add/rename: ASCII trim, optional literal authority extraction, then i6.v.e. */
    fun input(text: String): String {
        val trimmed = text.trim { it <= ' ' }
        val value = if (trimmed.contains("://")) fromUrl(trimmed) else trimmed
        var cursor = 0
        var end = value.indexOf('/')
        if (end < 0) end = value.indexOf(':')
        if (end < 0) end = value.length
        val output = StringBuilder()
        var labelEnd: Int
        while (true) {
            labelEnd = minOf(value.indexOf('.', cursor), end)
            if (labelEnd < 0) labelEnd = minOf(value.indexOf(':', cursor), end)
            if (labelEnd < 0) labelEnd = end
            output.append(SearchPunycode.label(value.substring(cursor, labelEnd)))
            val dot = value.indexOf('.', cursor)
            if (dot < 0 || dot >= end) break
            cursor = dot + 1
            output.append(value.substring(labelEnd, cursor))
        }
        output.append(value.substring(labelEnd))
        return output.toString()
    }
}
