package moe.via.browser.feature.scan

import android.content.Context
import moe.via.browser.R
import moe.via.browser.feature.search.resolve.SearchUrlResolver

/** c8.s6.j3 displays non-web payloads as escaped text; camera results never directly launch external apps. */
object ScanResultNavigation {
    fun destination(context: Context, raw: String): String {
        if (SearchUrlResolver.isAddress(raw)) {
            val address = SearchUrlResolver.normalize(raw)
            if (SearchUrlResolver.isHttp(address) || address.startsWith("file://", true) || address.startsWith("data:", true)) return address
        }
        val title = escape(context.getString(R.string.ref_scan_page_result_title))
        val html = "<head><title>$title</title><meta charset=\"utf-8\" name=\"viewport\" content=\"width=device-width, initial-scale=1, user-scalable=no, minimal-ui\"><style>body{font-family:-apple-system;text-align:center;margin-top:120px;color:black}@media(prefers-color-scheme:dark){body{color:white}}</style></head><body>${escape(raw)}</body>"
        // Encoding keeps a payload's #, %, commas and newlines inside the generated document.
        val encoded = try { java.net.URLEncoder.encode(html, "UTF-8").replace("+", "%20") } catch (_: Exception) { html }
        return "data:text/html," + encoded
    }

    private fun escape(text: String) = buildString(text.length) {
        text.forEach { append(when (it) { '"' -> "&quot;"; '<' -> "&lt;"; '>' -> "&gt;"; '&' -> "&amp;"; '\'' -> "&#039;"; else -> it.toString() }) }
    }
}
