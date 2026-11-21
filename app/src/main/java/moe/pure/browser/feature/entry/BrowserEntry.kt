package moe.pure.browser.feature.entry

/** Requests are issued only from an incoming Intent, never from a webpage URL callback. */
class BrowserEntryRequest internal constructor(
    val url: String,
    val mimeType: String?,
    val action: String?,
    internal val forceSearch: Boolean = false,
)

/** Prepared URL and its external-entry routing metadata. */
class BrowserEntry internal constructor(
    val url: String,
    val mimeType: String?,
    val originalUrl: String,
    val routeOnly: Boolean = false,
    /** Native only reuses an existing external-entry tab for a full matching HTTP(S) URL. */
    val reuseExisting: Boolean = false,
)

interface BrowserEntryPreparation {
    /** Cancel closes an active source stream and suppresses its callback. */
    fun cancel()
}

