package moe.pure.browser.feature.download.transfer

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import moe.pure.browser.feature.download.DownloadControl
import moe.pure.browser.feature.download.DownloadRepository
import moe.pure.browser.feature.download.DownloadSink

/** m5.e.h: a 2*N error budget, decreasing parallelism and 300ms waits between remaining rounds. */
internal class DownloadTransferGroup(private val repository: DownloadRepository, private val sink: DownloadSink,
    private val control: DownloadControl, private val networkAvailable: () -> Boolean, private val changed: (Long) -> Unit) {
    private data class Attempt(val control: DownloadControl, val future: Future<DownloadFailure?>)

    fun run(prepared: PreparedDownload) {
        val all = prepared.parts.map { it.index }
        if (all.isEmpty()) return
        val pool = Executors.newFixedThreadPool(all.size.coerceAtMost(8))
        val partTransfer = DownloadPartTransfer(repository, sink, networkAvailable, changed)
        var pending = all
        var budget = all.size * 2
        var parallel = all.size
        var lastFailure: DownloadFailure? = null
        var active = emptyList<Attempt>()
        try {
            while (pending.isNotEmpty() && budget > 0) {
                control.check()
                val current = pending.take(parallel)
                val deferred = pending.drop(parallel)
                val snapshot = repository.parts(prepared.task.id).associateBy { it.index }
                val submissions = mutableListOf<Attempt>()
                active = submissions
                current.forEach { index ->
                    val child = control.fork()
                    submissions += Attempt(child, pool.submit(Callable<DownloadFailure?> {
                        try {
                            val part = snapshot[index] ?: throw DownloadFailure(30)
                            partTransfer.run(prepared.task, part, prepared.metadata.ranges, child)
                            null
                        } catch (error: Exception) { DownloadFailure.from(error) }
                    }))
                }
                val retry = mutableListOf<Int>()
                var fatal: DownloadFailure? = null
                lastFailure = null
                active.forEachIndexed { index, attempt ->
                    val failure = try { attempt.future.get() } catch (error: InterruptedException) {
                        active.forEach { it.control.abort() }
                        Thread.interrupted()
                        active.forEach { runCatching { it.future.get() } }
                        Thread.currentThread().interrupt()
                        throw DownloadFailure(1, cause = error)
                    } catch (error: Exception) { DownloadFailure.from(error) }
                    if (fatal == null && failure != null) {
                        budget--; parallel = maxOf(1, parallel - 1); lastFailure = failure
                        if (failure.retryable) retry += current[index]
                        else { fatal = failure; active.drop(index + 1).forEach { it.control.abort() } }
                    }
                }
                active = emptyList()
                fatal?.let { throw it }
                pending = retry + deferred
                if (pending.isNotEmpty() && budget > 0) {
                    control.check()
                    try { Thread.sleep(DownloadRetryPolicy.BACKOFF_MS) } catch (error: InterruptedException) {
                        Thread.currentThread().interrupt(); throw DownloadFailure(1, cause = error)
                    }
                }
            }
            if (pending.isNotEmpty()) throw lastFailure ?: DownloadFailure(30)
        } finally {
            active.forEach { it.control.abort() }
            active.forEach { runCatching { it.future.get() } }
            pool.shutdownNow()
        }
    }
}
