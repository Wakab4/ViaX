package moe.pure.browser.feature.search.data

import moe.pure.browser.feature.search.model.SearchEngine
import java.util.Locale

/** Local search presets use direct URLs without remotely supplied referral parameters. */
object SearchBuiltins {
    fun defaultId(): Int = -3

    fun all(resourceLocale: Locale = Locale.getDefault()): List<SearchEngine> {
        val chinese = resourceLocale.language == "zh"
        val traditional = chinese && (resourceLocale.script == "Hant" || resourceLocale.country in listOf("TW", "HK"))
        return listOf(
            SearchEngine(-1, if (chinese && !traditional) "谷歌" else "Google", "https://www.google.com/search?q="),
            SearchEngine(-3, if (traditional) "微軟 Bing" else if (chinese) "必应" else "Bing", "https://www.bing.com/search?q="),
            SearchEngine(-7, "Yandex", "https://yandex.com/search/touch/?text=", "https://yandex.com/search/?text="),
            SearchEngine(-8, "Yahoo", "https://search.yahoo.com/search?p="),
            SearchEngine(-9, "StartPage", "https://startpage.com/do/search?query="),
            SearchEngine(-10, "DuckDuckGo", "https://duckduckgo.com/?q=")
        )
    }

    fun visible(resourceLocale: Locale = Locale.getDefault(), deviceLocale: Locale = Locale.getDefault()): List<SearchEngine> =
        all(resourceLocale).filter { it.id != -7 || deviceLocale.language.equals("ru", true) }

    fun suggestionProvider(engineId: Int, fallback: Int): Int = when (engineId) { -1 -> 1; -3 -> 3; else -> fallback }
}
