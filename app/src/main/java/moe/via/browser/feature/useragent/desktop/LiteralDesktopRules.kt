package moe.via.browser.feature.useragent.desktop

/** GP i6.h.l: retain all 36 entries, their order, duplicates and literal prefix/suffix behavior. */
internal object LiteralDesktopRules {
    private val rules = arrayOf(
        "://m.so.com" to "://www.so.com",
        "://m.news.so.com" to "://news.so.com",
        "://m.video.360kan.com" to "://video.360kan.com",
        "://m.image.so.com" to "://image.so.com",
        "://m.163.com" to "://www.163.com",
        "://m.thepaper.cn" to "://www.thepaper.cn",
        "://m.hao123.com" to "://www.hao123.com",
        "://m.sohu.com/a/" to "://www.sohu.com/a/",
        "://m.huanqiu.com" to "://www.huanqiu.com",
        "://m.guancha.cn" to "://www.guancha.cn",
        "://m.xs8.cn" to "://www.xs8.cn",
        "://m.autohome.com.cn" to "://www.autohome.com.cn",
        "://m.douban.com/movie/" to "://movie.douban.com/",
        "://m.douban.com/book/" to "://book.douban.com/",
        "://m.douban.com/music/" to "://music.douban.com/",
        "://m.douban.com/game/" to "://game.douban.com/",
        "://m.douban.com/people/" to "://www.douban.com/people/",
        "://m.hao123.com" to "://www.hao123.com",
        "://m.guoxuedashi.net" to "://www.guoxuedashi.net",
        "://h5.cloud.189.cn/main.html#/cloud/file/" to "://cloud.189.cn/web/main/file/folder/",
        "://new.m.taobao.com/detail.htm?" to "://item.taobao.com/item.htm?",
        "://h5.m.taobao.com/awp/core/detail.htm?" to "://item.taobao.com/item.htm?",
        "://main.m.taobao.com/security-h5-detail/home?" to "://item.taobao.com/item.htm?",
        "://main.m.taobao.com/search/index.html?" to "://s.taobao.com/search?",
        "://main.m.taobao.com" to "://www.taobao.com",
        "://item.m.jd.com/product/" to "://item.jd.com/",
        "://pro.m.jd.com" to "://pro.jd.com",
        "://m.jd.com" to "://www.jd.com",
        "://m.qidian.com" to "://www.qidian.com",
        "://gf2-bbs.sunborngame.com/m/" to "://gf2-bbs.sunborngame.com/",
        "://m.cngwzj.com/" to "://www.cngwzj.com/",
        "://m.twitch.tv/" to "://www.twitch.tv/",
        "://m.rumanhua.com/" to "://www.rumanhua.com/",
        "://m.zhtj.youth.cn/zhtj/" to "://zhtj.youth.cn/zhtj/",
        "://m.guancha.cn/" to "://www.guancha.cn/",
        "://m.cctv.com/" to "://www.cctv.com/"
    )

    fun rewrite(url: String, desktop: Boolean): String? {
        for ((mobile, large) in rules) {
            val source = if (desktop) mobile else large
            val at = url.indexOf(source)
            if (at > 0) return url.substring(0, at) + (if (desktop) large else mobile) + url.substring(at + source.length)
        }
        return null
    }
}
