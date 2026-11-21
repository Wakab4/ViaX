package moe.pure.browser.feature.network

import moe.pure.browser.feature.internalpages.render.GeneratedPageMarkup
import moe.pure.browser.feature.network.document.NetworkLogCss
import moe.pure.browser.feature.network.document.NetworkLogMarkup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** u9.c.h: render the final 64 recorded requests, newest first, then filter resources. */
object NetworkLogDocument {
    fun render(requests: List<PageRequest>, resources: Boolean, options: NetworkLogDocumentOptions): String {
        return buildString { writeTo(this, requests, resources, options) }
    }

    /** Streaming preserves Native's partial-file boundary if a later rendering step fails. */
    internal fun writeTo(output: Appendable, requests: List<PageRequest>, resources: Boolean,
        options: NetworkLogDocumentOptions) {
        with(output) {
            append(GeneratedPageMarkup.head(options.title))
            append("<style>").append(NetworkLogCss.render(options.style)).append("</style>")
            append("<body><div class='frosted-glass' id='gesture-indicator'></div><div id=\"content\">")
            var count = 0
            val rows = StringBuilder()
            if (requests.isNotEmpty()) {
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                for (index in requests.lastIndex downTo maxOf(0, requests.size - 64)) {
                    val entry = requests[index]
                    if (!resources || entry.resource) {
                        count++
                        rows.append(NetworkLogMarkup.row(entry, time.format(Date(entry.time))))
                    }
                }
            }
            val hint = when {
                count > 0 -> if (resources) options.resourcesHint else options.logHint
                resources -> options.resourcesEmpty + options.resourcesNote.replace("\n", "<br>")
                else -> options.empty
            }
            append(GeneratedPageMarkup.hint(hint))
            if (!resources && count > 0) append(NetworkLogMarkup.filter)
            append(rows)
            append("<script type=\"text/javascript\">")
            append(NetworkLogMarkup.boldDomainScript)
            if (!resources) append(NetworkLogMarkup.filterScript)
            append("</script></div></body></html>")
        }
    }
}
