package moe.pure.browser.feature.useragent.desktop

/** GP i6.h.e/f/g/h/i. Numeric offsets are the original string offsets. */
internal object BaiduDesktopUrls {
    fun search(url: String): String? {
        val search = url.indexOf(".baidu.com/sf/vsearch?pd=")
        if (search <= 0) {
            val at = url.indexOf("://m.baidu.com")
            if (at <= 0) return null
            var suffix = url.substring(at + 14)
            if (!suffix.contains("ie=")) suffix = suffix.replace("/s?", "/s?ie=utf-8&")
            return url.substring(0, at) + "://www.baidu.com" + suffix
        }
        val categoryStart = search + 25
        val categoryEnd = url.indexOf('&', categoryStart)
        if (categoryEnd < 0) return null
        val category = url.substring(categoryStart, categoryEnd)
        val wordAt = url.indexOf("&word=", categoryEnd)
        if (wordAt < 0) return null
        val wordStart = wordAt + 6
        val wordEnd = url.indexOf('&', wordStart).takeIf { it >= 0 } ?: url.length
        val word = url.substring(wordStart, wordEnd)
        return when (category) {
            "tieba" -> "https://tieba.baidu.com/f?ie=utf-8&kw=$word"
            "wenku" -> "https://wenku.baidu.com/search?word=$word"
            "image_content" -> "https://image.baidu.com/search/index?tn=baiduimage&ie=utf-8&word=$word"
            "wenda_tab" -> "https://zhidao.baidu.com/search?ie=utf-8&word=$word"
            else -> null
        }
    }

    fun diskToDesktop(url: String): String? {
        val at = url.indexOf("://pan.baidu.com/wap/home")
        if (at <= 0) return null
        val pathAt = url.indexOf("/dir/", at + 25)
        if (pathAt <= 0) return "https://pan.baidu.com/disk/main#/index?category=all"
        val start = pathAt + 5
        val end = url.indexOf('?', start).takeIf { it >= 0 } ?: url.length
        return "https://pan.baidu.com/disk/main#/index?category=all&path=" + url.substring(start, end)
    }

    fun diskToMobile(url: String): String? {
        val at = url.indexOf("://pan.baidu.com/disk/main")
        if (at <= 0) return null
        val pathAt = url.indexOf("&path=", at + 26)
        if (pathAt <= 0) return "https://pan.baidu.com/wap/home#/"
        val start = pathAt + 6
        val end = url.indexOf('&', start).takeIf { it >= 0 } ?: url.length
        return "https://pan.baidu.com/wap/home#/dir/" + url.substring(start, end)
    }

    fun dictionaryToDesktop(url: String): String? {
        val at = url.indexOf("://hanyu.baidu.com/hanyu-page/")
        if (at <= 0) return null
        val word = DesktopQuery.value(url, "wd=") ?: return null
        return when {
            url.startsWith("term/detail", at + 30) -> "https://hanyu.baidu.com/s?wd=$word&device=pc"
            url.startsWith("zici/s", at + 30) -> "https://hanyu.baidu.com/zici/s?wd=$word"
            else -> null
        }
    }

    fun dictionaryToMobile(url: String): String? {
        val at = url.indexOf("://hanyu.baidu.com/")
        if (at <= 0 || url.startsWith("hanyu-page/", at + 19)) return null
        val word = DesktopQuery.value(url, "wd=") ?: return null
        return when {
            url.startsWith("s", at + 19) -> "https://hanyu.baidu.com/hanyu-page/term/detail?wd=$word"
            url.startsWith("zici/s", at + 19) -> "https://hanyu.baidu.com/hanyu-page/zici/s?wd=$word"
            else -> null
        }
    }
}
