package moe.pure.browser.feature.translation

import java.util.Locale

/** Reference order and exact language codes, including tl and zh-TW. */
class TranslationLanguages(private val resourceLocale: Locale) {
    val targets: List<TranslationLanguage> = names.map { (code, fallback) ->
        val display = if (code == "zh-TW") {
            if (resourceLocale.language == "zh") "繁體中文" else "Chinese (Traditional)"
        } else runCatching { Locale(code).getDisplayLanguage(resourceLocale) }.getOrNull()
        TranslationLanguage(code, display?.takeIf { it.isNotBlank() && !it.equals(code, true) } ?: fallback, fallback)
    }
    val sources: List<TranslationLanguage> = listOf(TranslationLanguage("auto", "检测语言", "Auto")) + targets

    fun source(code: String?): TranslationLanguage = sources.firstOrNull { it.code == code } ?: sources.first()
    fun target(code: String?): TranslationLanguage = targets.firstOrNull { it.code == code } ?: targets.first()

    /** Native requests zh_HANT for TW, which intentionally misses the zh-TW catalogue entry. */
    fun defaultTarget(): TranslationLanguage {
        val language = TranslationPreferences.languageCode(resourceLocale.language)
        return target(if (language == "zh" && resourceLocale.country == "TW") "zh_HANT" else language)
    }

    companion object {
        private val names = listOf(
            "ar" to "Arabic", "az" to "Azerbaijani", "be" to "Belarusian", "bn" to "Bengali",
            "cs" to "Czech", "de" to "German", "en" to "English", "es" to "Spanish",
            "fa" to "Persian", "tl" to "Filipino", "fr" to "French", "hi" to "Hindi",
            "hr" to "Croatian", "hu" to "Hungarian", "id" to "Indonesian", "it" to "Italian",
            "ja" to "Japanese", "ko" to "Korean", "lt" to "Lithuanian", "ne" to "Nepali",
            "pl" to "Polish", "pt" to "Portuguese", "ro" to "Romanian", "ru" to "Russian",
            "sl" to "Slovenian", "ta" to "Tamil", "tr" to "Turkish", "uk" to "Ukrainian",
            "uz" to "Uzbek", "vi" to "Vietnamese", "zh" to "Chinese", "zh-TW" to "Chinese (Traditional)"
        )
    }
}
