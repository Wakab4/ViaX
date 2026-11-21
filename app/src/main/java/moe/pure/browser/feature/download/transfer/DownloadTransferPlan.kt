package moe.pure.browser.feature.download.transfer

import moe.pure.browser.feature.download.DownloadMetadata
import moe.pure.browser.feature.download.DownloadPart
import moe.pure.browser.feature.download.DownloadRepository
import moe.pure.browser.feature.download.DownloadSink
import moe.pure.browser.feature.download.DownloadTask

internal data class PreparedDownload(val task: DownloadTask, val metadata: DownloadMetadata, val parts: List<DownloadPart>)

/** m5.e.m/n/r: persisted chunks and destination length are prepared before each transfer pass. */
internal class DownloadTransferPlan(private val repository: DownloadRepository, private val sink: DownloadSink) {
    fun prepare(task: DownloadTask, metadata: DownloadMetadata, connections: Int, reuse: Boolean, fingerprint: DownloadTask = task): PreparedDownload {
        try {
            val ranged = metadata.ranges
            val options = metadata.copy(ranges = ranged)
            val old = if (reuse) repository.parts(task.id) else emptyList()
            val size = sink.size()
            val reusable = ranged && old.isNotEmpty() && fingerprint.total == metadata.total && fingerprint.etag == metadata.etag &&
                fingerprint.lastModified == metadata.lastModified && old.all {
                    it.received >= 0 && it.length > 0 && it.received <= it.length &&
                        (it.received == 0L || size >= it.start + it.received)
                }
            val parts = if (reusable) old else partition(metadata.total, if (ranged) connections else 1, ranged)
            repository.prepare(task.id, options, parts, if (ranged || !metadata.ready) connections else 1)
            sink.prepareLength(if (ranged) metadata.total else 0)
            return PreparedDownload(repository.get(task.id) ?: throw DownloadFailure(1), options, parts)
        } catch (error: Exception) {
            if (error is DownloadFailure) throw error
            throw DownloadFailure(12, cause = error)
        }
    }
    private fun partition(total: Long, connections: Int, ranged: Boolean): List<DownloadPart> {
        if (!ranged || total <= 0) return listOf(DownloadPart(0, 0, -1))
        val count = minOf(connections.coerceIn(1, 8).toLong(), total).toInt()
        val length = total / count
        return List(count) { index -> DownloadPart(index, index * length, if (index == count - 1) total - 1 else (index + 1) * length - 1) }
    }
}
