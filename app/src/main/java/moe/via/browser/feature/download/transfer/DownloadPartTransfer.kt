package moe.via.browser.feature.download.transfer

import java.io.IOException
import kotlin.math.abs
import moe.via.browser.feature.download.http.DownloadHttp
import moe.via.browser.feature.download.model.DownloadPart
import moe.via.browser.feature.download.data.DownloadRepository
import moe.via.browser.feature.download.model.DownloadTask

/** m5.d: one attempt changes only its own chunk; retry ownership belongs to the transfer group. */
internal class DownloadPartTransfer(private val repository: DownloadRepository, private val sink: DownloadSink,
    private val networkAvailable: () -> Boolean, private val changed: (Long) -> Unit) {
    fun run(task: DownloadTask, part: DownloadPart, resumable: Boolean, control: DownloadControl) {
        if (part.length > 0 && part.received == part.length) return
        control.check()
        if (!networkAvailable()) throw DownloadFailure(32)
        var received = if (resumable) part.received else 0L
        if (received != part.received) repository.progress(task.id, part.index, received)
        val output = try { sink.open(part.start + received) } catch (error: Exception) { throw DownloadFailure(12, cause = error) }
        var committed = received
        var lastCommit = System.nanoTime()
        fun commit() {
            try { output.flush() } catch (error: IOException) { throw DownloadFailure(12, cause = error) }
            repository.progress(task.id, part.index, received)
            committed = received; lastCommit = System.nanoTime(); changed(task.id)
        }
        try {
            val response = DownloadHttp(control).open(task, part, received)
            try {
                val expected = response.length
                if (part.length <= 0 && expected > 0) repository.partLength(task.id, part.index, part.start, expected)
                val buffer = ByteArray(4096)
                while (true) {
                    control.check()
                    val count = try { response.input.read(buffer) } catch (error: IOException) { throw DownloadFailure(20, cause = error) }
                    if (count < 0) break
                    try { output.write(buffer, count) } catch (error: IOException) { throw DownloadFailure(12, cause = error) }
                    received += count
                    if (System.nanoTime() - lastCommit > 1_000_000_000L) commit()
                    if (expected > 0 && received >= expected) break
                }
                if (expected > 0 && abs(received - expected) > 1) throw DownloadFailure(31)
            } finally { runCatching { response.close() } }
        } finally {
            try { if (received != committed) commit() } finally { runCatching { output.close() } }
        }
    }
}
