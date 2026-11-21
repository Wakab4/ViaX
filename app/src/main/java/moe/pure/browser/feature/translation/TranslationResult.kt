package moe.pure.browser.feature.translation

/** Source retains the requested code; the native backend does not consume detected language. */
data class TranslationResult(val query: String, val translated: String, val source: String, val target: String)
