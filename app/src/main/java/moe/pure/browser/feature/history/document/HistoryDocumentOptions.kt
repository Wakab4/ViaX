package moe.pure.browser.feature.history.document

/** Resolve localized strings and background contrast in the window before scheduling generation. */
data class HistoryDocumentOptions(
    val title: String,
    val empty: String,
    val incognitoHint: String,
    val isRtl: Boolean,
    val isNight: Boolean,
    val isLight: Boolean,
    val globalIncognito: Boolean
)
