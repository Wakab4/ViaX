package moe.via.browser.feature.library.history.data

/** Native history keeps row identity separate from its nullable URL and title. */
data class HistoryRecord(
    val id: Long = 0,
    val url: String? = null,
    val title: String? = null,
    val updatedAtSeconds: Long = 0
)

data class HistoryRowsRead(val rows: List<HistoryRecord>, val failure: Exception? = null)
data class HistoryTimeCount(val sinceSeconds: Long, val count: Int)
