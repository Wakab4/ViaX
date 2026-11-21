package moe.pure.browser.feature.download.transfer

import moe.pure.browser.feature.download.DownloadMetadata

/** m5.e.h/call/q: chunk budget, concurrency decay and the second transfer pass are distinct. */
internal object DownloadRetryPolicy {
    const val BACKOFF_MS = 300L
    fun connections(total: Long, limit: Int): Int = if (total <= 0) 1
        else minOf(limit.coerceIn(1, 8), ((total - 1) / 1048576 + 1).coerceAtMost(8).toInt())

    data class Fallback(val metadata: DownloadMetadata, val connections: Int)
    fun fallback(error: DownloadFailure, metadata: DownloadMetadata, connections: Int): Fallback = when {
        (error.code == 24 || error.code == 416) && !(metadata.ranges && connections == 1) ->
            Fallback(metadata.copy(ranges = false), 1)
        error.code in 400..499 && connections != 1 -> Fallback(metadata, 1)
        else -> Fallback(metadata, connections)
    }
}
