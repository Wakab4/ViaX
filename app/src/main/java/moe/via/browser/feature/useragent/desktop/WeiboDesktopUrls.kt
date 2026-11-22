package moe.via.browser.feature.useragent.desktop

/** GP i6.h.x/y: article URLs and grouped Weibo IDs are independent of the literal host table. */
internal object WeiboDesktopUrls {
    fun toDesktop(url: String): String? {
        val article = url.indexOf("://card.weibo.com/article/m/show/id/")
        if (article > 0) {
            val start = article + 36
            val end = url.indexOf('?', start).takeIf { it >= 0 } ?: url.length
            return "https://weibo.com/ttarticle/p/show?id=" + url.substring(start, end)
        }
        val detail = url.indexOf("://m.weibo.cn/detail/")
        if (detail <= 0) {
            val home = url.indexOf("://m.weibo.cn")
            if (home <= 0) return null
            return url.substring(0, home) + "://weibo.com" + url.substring(home + 13)
        }
        val start = detail + 21
        var end = url.indexOf('?', start)
        if (end == -1) end = url.indexOf('/', start)
        if (end == -1) end = url.length
        return "https://weibo.com/0/" + WeiboBase62.decimalToShort(url.substring(start, end)) + url.substring(end)
    }

    fun toMobile(url: String): String? {
        val article = url.indexOf("://weibo.com/ttarticle/p/show?id=")
        if (article > 0) {
            val start = article + 33
            val end = url.indexOf('&', start).takeIf { it >= 0 } ?: url.length
            return "https://card.weibo.com/article/m/show/id/" + url.substring(start, end)
        }
        if (url.indexOf("://weibo.com/newlogin?") > 0) return "https://m.weibo.cn/"
        val at = url.indexOf("://weibo.com")
        if (at <= 0) return null
        // The Native +1 checks the first slash of ://, not the slash after the authority.
        if (url.length > at + 1 && url[at + 1] == '/') {
            val userStart = at + 13
            val userEnd = url.indexOf('/', userStart)
            if (userEnd > 0) {
                val user = url.substring(userStart, userEnd)
                if (user.isNotEmpty() && user.all { it in '0'..'9' }) {
                    val start = userEnd + 1
                    var end = url.indexOf('?', start)
                    if (end == -1) end = url.indexOf('/', start)
                    if (end == -1) end = url.length
                    return "https://m.weibo.cn/detail/" + WeiboBase62.shortToDecimal(url.substring(start, end)) + url.substring(end)
                }
            }
        }
        return url.substring(0, at) + "://m.weibo.cn" + url.substring(at + 12)
    }
}
