package moe.pure.browser.feature.download

import java.io.EOFException

class PayloadDownloadWorker(private val repository: DownloadRepository, private val payloads: DownloadPayloads,
    private val sink: DownloadSink, private val control: DownloadControl, private val changed: (Long) -> Unit = {}) {
    fun run(id: Long, restart: Boolean = false): DownloadState {
        val task = repository.get(id) ?: return DownloadState.CANCELED
        try {
            control.check()
            val payload = payloads.get(task.payload ?: error("缺少页面下载内容")) ?: error("页面下载内容已失效")
            repository.state(id, DownloadState.RUNNING)
            val old = repository.parts(id).singleOrNull()
            var bytes = if (!restart && old != null && old.start == 0L && old.end == payload.size - 1 && old.received <= payload.size && sink.size() >= old.received) old.received else 0L
            if (bytes == 0L) sink.truncate(0)
            repository.prepare(id, DownloadMetadata(payload.size, false, null, null, payload.mime.substringBefore(';')), if (payload.size == 0L) emptyList() else listOf(DownloadPart(0, 0, payload.size - 1, bytes)))
            if (payload.size > bytes) {
                payloads.input(payload.token).use { input ->
                    var skipped = 0L
                    while (skipped < bytes) {
                        control.check()
                        val count = input.skip(bytes - skipped)
                        if (count > 0) skipped += count else { if (input.read() < 0) throw EOFException("页面下载内容不完整"); skipped++ }
                    }
                    sink.open(bytes).use { output ->
                        var checkpoint = bytes; var last = System.nanoTime()
                        fun commit() { output.flush(); repository.progress(id, 0, bytes); checkpoint = bytes; last = System.nanoTime(); changed(id) }
                        try {
                            val buffer = ByteArray(32 * 1024)
                            while (bytes < payload.size) {
                                control.check(); val count = input.read(buffer, 0, minOf(buffer.size.toLong(), payload.size - bytes).toInt())
                                if (count < 0) throw EOFException("页面下载内容不完整")
                                output.write(buffer, count); bytes += count
                                if (System.nanoTime() - last >= 250_000_000L) commit()
                            }
                        } finally { if (checkpoint != bytes) commit() }
                    }
                }
            }
            control.check(); repository.complete(id); changed(id); return DownloadState.COMPLETE
        } catch (error: Exception) {
            val state = control.requestedState ?: DownloadState.FAILED
            repository.state(id, state, if (state == DownloadState.FAILED) error.message else null); changed(id); return state
        }
    }
}
