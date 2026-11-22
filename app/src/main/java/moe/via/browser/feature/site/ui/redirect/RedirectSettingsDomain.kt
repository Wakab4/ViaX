package moe.via.browser.feature.site.ui.redirect

import moe.via.browser.feature.search.resolve.SearchPunycode
import java.net.IDN
import java.util.Locale

/** ba.c.f / i6.v.e followed by kb.c: preserve literal keys and the original region boundaries. */
internal object RedirectSettingsDomain {
    fun input(value: String): String? {
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
            cursor = dot + 1
            output.append(value.substring(labelEnd, cursor))
        }
        output.append(value.substring(labelEnd))
        return output.toString().takeIf { normalize(it).isNotEmpty() }
    }

    fun normalize(value: String): String = value.trim { it <= ' ' }.lowercase(Locale.ROOT)

    fun display(value: String): String {
        val decoded = runCatching { IDN.toUnicode(value) }.getOrNull()
        return if (decoded.isNullOrEmpty() || decoded == value) value else "$value ($decoded)"
    }

    /** k8.p compares labels from the right, including its original end-index arithmetic. */
    val order = Comparator<String> { first, second ->
        var left = first.length; var right = second.length
        var leftStart = left; var rightStart = right
        while (left > 0 && right > 0) {
            val leftDot = first.lastIndexOf('.', left); leftStart = leftDot + 1
            val rightDot = second.lastIndexOf('.', right); rightStart = rightDot + 1
            val width = left - leftStart
            for (index in 0 until minOf(width, right - rightStart)) {
                val difference = first[leftStart + index].code - second[rightStart + index].code
                if (difference != 0) return@Comparator difference
            }
            val difference = width - right + rightStart
            if (difference != 0) return@Comparator difference
            left = leftDot - 1; right = rightDot - 1
        }
        leftStart - rightStart
    }
}
