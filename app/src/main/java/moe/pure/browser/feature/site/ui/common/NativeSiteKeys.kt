package moe.pure.browser.feature.site.ui.common

import moe.pure.browser.feature.site.ui.redirect.RedirectSettingsDomain
import moe.pure.browser.feature.search.resolve.SearchPunycode

/** Keep original encoded DB keys for binary pages; other Native models normalize before persistence. */
internal object NativeSiteKeys {
    fun input(value: String, normalized: Boolean = false): String? {
        if (value.isEmpty()) return null
        val protocol = value.indexOf("://")
        var cursor = if (protocol < 0) 0 else protocol + 3
        var end = value.indexOf('/', cursor)
        if (end < 0) end = value.indexOf(':', cursor)
        if (end < 0) end = value.length
        val output = StringBuilder(value.substring(0, cursor))
        var labelEnd: Int
        while (true) {
            labelEnd = minOf(value.indexOf('.', cursor), end)
            if (labelEnd < 0) labelEnd = minOf(value.indexOf(':', cursor), end)
            if (labelEnd < 0) labelEnd = end
            output.append(SearchPunycode.label(value.substring(cursor, labelEnd)))
            val dot = value.indexOf('.', cursor)
            if (dot < 0 || dot >= end) break
            cursor = dot + 1; output.append(value.substring(labelEnd, cursor))
        }
        output.append(value.substring(labelEnd))
        return if (normalized) normalize(output.toString()).takeIf(String::isNotEmpty) else output.toString()
    }
    fun normalize(value: String) = RedirectSettingsDomain.normalize(value)
    fun display(value: String) = RedirectSettingsDomain.display(value)
    val order get() = RedirectSettingsDomain.order
}
