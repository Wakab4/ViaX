package moe.via.browser.feature.library.ui

/** Native g6.p.j and i6.g0.d operate on UTF-16 strings, without URI normalization. */
internal object LibraryDisplayText {
    fun title(value: String): String = if (value.length > 256) value.substring(0, 256) + "..." else value

    fun url(value: String): String {
        if (value.isEmpty()) return ""
        val web = value.startsWith("http://", true) || value.startsWith("https://", true)
        val start = if (web) value.indexOf("://") + 3 else 0
        val end = if (web && value.indexOf('/', start) == value.lastIndex) value.lastIndex else value.length
        return value.substring(start, minOf(start + 256, end))
    }
}
