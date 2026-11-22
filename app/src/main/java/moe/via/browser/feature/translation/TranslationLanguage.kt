package moe.via.browser.feature.translation

class TranslationLanguage(val code: String, val label: String, val englishName: String = label) {
    override fun equals(other: Any?) = other is TranslationLanguage && code == other.code
    override fun hashCode() = code.hashCode()
    override fun toString() = label
}
