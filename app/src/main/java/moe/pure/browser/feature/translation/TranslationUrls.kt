package moe.pure.browser.feature.translation

import java.net.URLEncoder

object TranslationUrls {
    fun baidu(url: String?, locale: String?): String? {
        if (url.isNullOrEmpty() || locale.isNullOrEmpty()) return null
        return "https://fanyi.baidu.com/transpage?query=${encode(url)}&source=url&ie=utf8&from=auto&to=${encode(locale.substringBefore('-'))}&render=1"
    }

    fun google(url: String?, locale: String?): String? {
        if (url.isNullOrEmpty() || locale.isNullOrEmpty()) return null
        return "https://translate.google.com/translate?sl=auto&tl=${encode(locale)}&u=${encode(url)}"
    }

    fun yodao(url: String?, locale: String?): String? {
        if (url.isNullOrEmpty() || locale.isNullOrEmpty()) return null
        return "http://webtrans.yodao.com/webTransPc/index.html#/?url=${encode(url)}&from=auto&to=${encode(locale.substringBefore('-'))}&type=1"
    }

    internal fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
