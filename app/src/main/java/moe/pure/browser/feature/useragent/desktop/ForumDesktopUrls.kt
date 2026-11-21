package moe.pure.browser.feature.useragent.desktop

/** GP i6.h.o/p/q/r: Hupu and ITHome keep their individual path and suffix rules. */
internal object ForumDesktopUrls {
    private val hupuSections = arrayOf("nba", "cba", "soccer", "gg")

    fun hupuToDesktop(url: String): String? {
        val at = url.indexOf("://m.hupu.com/bbs/")
        if (at <= 0) {
            val home = url.indexOf("://m.hupu.com/")
            if (home <= 0) return null
            val start = home + 14
            if (start == url.length) return url.substring(0, home) + "://www.hupu.com/"
            val section = url.substring(start)
            return hupuSections.firstOrNull { it == section }?.let { url.substring(0, home) + "://$it.hupu.com/" }
        }
        val start = at + 18
        var end = url.indexOf(".html", start)
        if (end > 0) return url.substring(0, at) + "://bbs.hupu.com/" + url.substring(start)
        if (end < 0) end = url.indexOf('?', start)
        if (end < 0) {
            // The original returns the input itself for an existing nested path.
            if (url.indexOf('/', start) > 0) return url
            end = url.length
        }
        return url.substring(0, at) + "://bbs.hupu.com/" + url.substring(start, end) + ".html" + url.substring(end)
    }

    fun hupuToMobile(url: String): String? {
        val at = url.indexOf("://bbs.hupu.com/")
        if (at > 0) return url.substring(0, at) + "://m.hupu.com/bbs/" + url.substring(at + 16)
        val hostEnd = url.indexOf(".hupu.com/")
        if (hostEnd <= 0 || hostEnd + 10 != url.length) return null
        val scheme = url.indexOf("://")
        if (scheme <= 0) return null
        val start = scheme + 3
        val section = url.substring(start, hostEnd)
        if (section == "www") return url.substring(0, start) + "m.hupu.com/"
        return hupuSections.firstOrNull { it == section }?.let { url.substring(0, start) + "m.hupu.com/$it" }
    }

    fun ithomeToDesktop(url: String): String? {
        val at = url.indexOf("://m.ithome.com/html/")
        if (at > 0) {
            val start = at + 21
            val end = url.indexOf(".htm", start)
            if (end > 0 && end - at - 21 == 6)
                return url.substring(0, at) + "://www.ithome.com/0/" + url.substring(start, at + 24) + "/" + url.substring(end - 3)
        }
        val home = url.indexOf("://m.ithome.com/")
        if (home <= 0) return null
        val start = home + 16
        if (start != url.length && (start >= url.length || url[start] != '#')) return null
        return url.substring(0, home) + "://www.ithome.com/"
    }

    fun ithomeToMobile(url: String): String? {
        val at = url.indexOf("://www.ithome.com/0/")
        if (at > 0) {
            val start = at + 20
            val end = url.indexOf(".htm", start)
            if (end > 0 && end - at - 20 == 7)
                return url.substring(0, at) + "://m.ithome.com/html/" + url.substring(start, at + 23) + url.substring(end - 3)
        }
        val home = url.indexOf("://www.ithome.com/")
        if (home <= 0) return null
        val start = home + 18
        if (start != url.length && (start >= url.length || url[start] != '#')) return null
        return url.substring(0, home) + "://m.ithome.com/"
    }
}
