package moe.via.browser.browser.ui.tabs.switcher

import android.content.Context
import android.graphics.Bitmap
import moe.via.browser.R
import moe.via.browser.feature.home.HomeDocument
import moe.via.browser.feature.search.resolve.SearchTemplate
import moe.via.browser.browser.session.BrowserTab

/** f8.y binds resident metadata without creating dormant WebViews. */
internal data class TabSwitcherItem(val id: String, val title: String, val url: String, val icon: Bitmap?, val selected: Boolean, val home: Boolean) {
    companion object {
        fun capture(context: Context, tab: BrowserTab, selected: Boolean): TabSwitcherItem {
            val resident = tab.liveView
            val url = if (resident == null) tab.url else resident.url.orEmpty()
            val title = if (resident == null) tab.title else resident.title.orEmpty()
            return TabSwitcherItem(tab.id, decoratedTitle(title, url).ifEmpty { url }
                .ifEmpty { context.getString(R.string.ref_tab_switcher_untitled) }, url,
                resident?.favicon?.takeUnless(Bitmap::isRecycled), selected, HomeDocument.isHome(context, url))
        }

        /** z8.b0.G, including its exact title and URL-length guards. */
        private fun decoratedTitle(title: String, url: String): String {
            if (url.length > 38 && (title == "QQ浏览器搜索" || title == "QQ 浏览器搜索")) {
                SearchTemplate.extract(url, "https://wap.sogou.com/web/sl?keyword=")?.takeIf(String::isNotEmpty)
                    ?.let { return "$it - $title" }
            } else if (url.length > 47 && (title == "秘塔AI搜索" || title == "秘塔 AI 搜索")) {
                SearchTemplate.extract(url, "https://metaso.cn/search/3333?q=")?.takeIf(String::isNotEmpty)
                    ?.let { return "$it - $title" }
            }
            if (title != "网页无法打开" && title != "Webpage not available") return title
            val scheme = url.indexOf("://")
            if (scheme < 0) return ""
            val start = scheme + 3
            val host = url.substring(start, url.indexOf('/', start).takeIf { it >= 0 } ?: url.length).substringBefore(':')
            val dot = host.indexOf('.')
            return if ((dot == 1 && host.startsWith("m.") || dot == 3 && host.startsWith("www.")) && host.indexOf('.', dot + 1) >= 0)
                host.substring(dot + 1) else host
        }
    }
}
