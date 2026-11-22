package moe.via.browser.feature.library.history

import moe.via.browser.feature.library.LibraryEntry

/** Native returns rows accumulated before a DAO Exception; the failure remains diagnostic, not a retry state. */
data class HistoryRead(val rows: List<LibraryEntry>, val failure: Exception? = null)
data class HistoryPeriod(val index: Int, val sinceSeconds: Long, val count: Int)

internal object HistoryQuery {
    data class Selection(val sql: String, val arguments: Array<String>)
    /** z8.i0.d: raw text, conditional percent/underscore escaping, and a distinct HTTP prefix branch. */
    fun selection(raw: String): Selection? {
        if (raw.isEmpty()) return null
        val escaped = raw.any { it == '%' || it == '_' }
        val query = if (escaped) buildString { raw.forEach { if (it == '%' || it == '_') append('\\'); append(it) } } else raw
        val escape = if (escaped) " escape '\\'" else ""
        return if (query.startsWith("http://", true) || query.startsWith("https://", true)) {
            Selection("url like ?$escape", arrayOf("$query%"))
        } else Selection("url like ?$escape or title like ?$escape or title like ?$escape", arrayOf("%_$query%", "%_$query%", "$query%"))
    }
}
