package moe.pure.browser.core

import moe.pure.browser.feature.search.resolve.SearchTemplate
import moe.pure.browser.web.compat.NativePageAddress

/** z8.b0.G decorates only these exact browser/search error titles. */
object NativePageTitles {
    fun decorate(title: String?, url: String?): String? {
        if (title == null || url == null) return title
        if (url.length > 38 && (title == "QQ浏览器搜索" || title == "QQ 浏览器搜索")) {
            SearchTemplate.extract(url, "https://wap.sogou.com/web/sl?keyword=", NativeUrlDecoder::guarded)
                ?.takeIf(String::isNotEmpty)?.let { return "$it - $title" }
        } else if (url.length > 47 && (title == "秘塔AI搜索" || title == "秘塔 AI 搜索")) {
            SearchTemplate.extract(url, "https://metaso.cn/search/3333?q=", NativeUrlDecoder::guarded)
                ?.takeIf(String::isNotEmpty)?.let { return "$it - $title" }
        }
        if (title != "网页无法打开" && title != "Webpage not available") return title
        val host = NativePageAddress.authority(url)
        val dot = host.indexOf('.')
        return if ((dot == 1 && host[0] == 'm' || dot == 3 && host.startsWith("www")) &&
            host.indexOf('.', dot + 1) >= 0) host.substring(dot + 1) else host
    }
}
