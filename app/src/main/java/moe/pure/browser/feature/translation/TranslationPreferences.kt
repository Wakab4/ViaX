package moe.pure.browser.feature.translation

import android.content.Context
import android.os.LocaleList
import java.util.Locale

/** The GP build's regional branch depends on the device country and selected search engine. */
class TranslationPreferences(context: Context) {
    val resourceLocale: Locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
    val deviceLocaleTag: String get() = localeTag(LocaleList.getAdjustedDefault()[0] ?: Locale.getDefault())
    val showRegionalProviders: Boolean get() = Locale.getDefault().country.equals("CN", true)
    fun useEdgeByDefault(searchEngineId: Int) = showRegionalProviders && searchEngineId != -1

    companion object {
        fun languageCode(language: String): String = when (language) {
            "in" -> "id"; "iw" -> "he"; "ji" -> "yi"; "jw" -> "jv"; "tl" -> "fil"
            else -> language
        }

        fun localeTag(locale: Locale): String {
            val language = languageCode(locale.language)
            if (language == "no" && locale.country == "NO" && locale.variant == "NY") return "nn-NO"
            return language + locale.country.takeIf(String::isNotEmpty)?.let { "-$it" }.orEmpty()
        }
    }
}
