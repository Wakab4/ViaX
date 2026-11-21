package moe.pure.browser.web.error

/** Diagnostic metadata only. The active WebView provider renders its own network error document. */
data class BrowserPageFailure(val code: Int, val description: String?, val failingUrl: String)
