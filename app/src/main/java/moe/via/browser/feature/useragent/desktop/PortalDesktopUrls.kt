package moe.via.browser.feature.useragent.desktop

/** GP i6.h.j/k/t/u/v/w/z; no generic mobile-subdomain replacement is applied. */
internal object PortalDesktopUrls {
    fun copyrightToDesktop(url: String): String? {
        val at = url.indexOf("://www.ccopyright.com.cn/mobile/")
        if (at <= 0) return null
        return url.substring(0, at) + "://www.ccopyright.com.cn/" + url.substring(at + 32)
    }

    fun copyrightToMobile(url: String): String? {
        val at = url.indexOf("://www.ccopyright.com.cn/")
        if (at <= 0) return null
        val start = at + 25
        val end = at + 32
        if (url.length >= end && url.substring(start, end) == "mobile/") return null
        return url.substring(0, start) + "mobile/" + url.substring(start)
    }

    fun mailToDesktop(url: String): String? =
        if (url.indexOf("://wap.mail.qq.com/") > 0) "https://mail.qq.com/" else null

    fun mailToMobile(url: String): String? =
        if (url.indexOf("://mail.qq.com/") > 0) "https://wap.mail.qq.com/" else null

    fun toutiaoToDesktop(url: String): String? =
        if (url.indexOf("://so.toutiao.com/search?") <= 0 || url.contains("dvpf=pc")) null else "$url&dvpf=pc"

    fun toutiaoToMobile(url: String): String? =
        if (url.indexOf("://so.toutiao.com/search?") > 0) DesktopQuery.remove(url, "dvpf=pc") else null

    fun youtubeToMobile(url: String): String? {
        val at = url.indexOf("://www.youtube.com/")
        if (at <= 0) return null
        val mobile = url.substring(0, at) + "://m.youtube.com/" + url.substring(at + 19)
        // A missing app=desktop returns null, so the following rules still get the original URL.
        return DesktopQuery.remove(mobile, "app=desktop")
    }
}
