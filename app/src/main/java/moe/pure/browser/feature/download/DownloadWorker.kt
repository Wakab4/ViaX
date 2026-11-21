package moe.pure.browser.feature.download

import moe.pure.browser.feature.download.transfer.DownloadFailure
import moe.pure.browser.feature.download.transfer.DownloadRetryPolicy
import moe.pure.browser.feature.download.transfer.DownloadTransferGroup
import moe.pure.browser.feature.download.transfer.DownloadTransferPlan
import moe.pure.browser.feature.download.http.NativeDownloadProbe

/** Task state orchestration only. Planning, chunk IO, retry rounds and HTTP remain separate owners. */
class DownloadWorker(private val repository: DownloadRepository, private val sink: DownloadSink,
    private val control: DownloadControl, private val networkAvailable: () -> Boolean,
    private val changed: (Long) -> Unit = {}) {
    fun run(id: Long, restart: Boolean = false): DownloadState {
        var task = repository.get(id) ?: return DownloadState.CANCELED
        val fingerprint = task
        if (!restart && task.state == DownloadState.COMPLETE) return task.state
        try {
            control.check(); repository.state(id, DownloadState.RUNNING); notify(id)
            val metadata = if (!restart && task.rangeMode in 0..1) {
                DownloadMetadata(task.total, task.rangeMode == 1, task.etag, task.lastModified, task.mime)
            } else {
                if (!networkAvailable()) throw DownloadFailure(32)
                val probed = NativeDownloadProbe(repository, control).run(task)
                task = probed.task; notify(id)
                probed.metadata
            }
            val connections = if (!metadata.ready) task.connections else if (metadata.ranges) DownloadRetryPolicy.connections(metadata.total, task.connections) else 1
            val plan = DownloadTransferPlan(repository, sink)
            val group = DownloadTransferGroup(repository, sink, control, networkAvailable, ::notify)
            try {
                group.run(plan.prepare(task, metadata, connections, reuse = !restart, fingerprint = fingerprint))
            } catch (error: Exception) {
                val fallback = DownloadRetryPolicy.fallback(DownloadFailure.from(error), metadata, connections)
                // Native clears chunks for its second pass even when retaining the same connection policy.
                repository.clearParts(id)
                control.check()
                group.run(plan.prepare(task, fallback.metadata, fallback.connections, reuse = false))
            }
            control.check(); repository.complete(id); notify(id)
            return DownloadState.COMPLETE
        } catch (error: Exception) {
            val failure = DownloadFailure.from(error)
            val state = control.requestedState ?: when (failure.code) {
                1 -> DownloadState.PAUSED
                32 -> DownloadState.WAITING_NETWORK
                else -> DownloadState.FAILED
            }
            repository.state(id, state, if (state == DownloadState.FAILED) failure.message else null); notify(id)
            return state
        }
    }
    private fun notify(id: Long) { runCatching { changed(id) } }
}
