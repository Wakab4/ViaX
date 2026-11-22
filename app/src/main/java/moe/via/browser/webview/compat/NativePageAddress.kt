package moe.via.browser.webview.compat

/** i6.i0.f uses the original string's authority and first colon, without URI normalization. */
object NativePageAddress {
    fun authority(url: String?): String {
        if (url.isNullOrEmpty()) return ""
        val separator = url.indexOf("://")
        if (separator < 0) return ""
        val start = separator + 3
        val end = url.indexOf('/', start).takeIf { it >= 0 } ?: url.length
        return url.substring(start, end).substringBefore(':')
    }

    /** s6.va's common gate for its built-in page compatibility scripts. */
    fun acceptsPolicy(url: String?) = !url.isNullOrEmpty() && !url.startsWith("file://", true) && authority(url).isNotEmpty()
}
