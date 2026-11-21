package moe.pure.browser.feature.download

import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class DownloadControl private constructor(private val parent: DownloadControl?) {
    constructor() : this(null)
    private val connections = ConcurrentHashMap.newKeySet<HttpURLConnection>()
    private val requested = AtomicReference<DownloadState?>()
    private val aborted = AtomicBoolean()
    val requestedState: DownloadState? get() = requested.get() ?: parent?.requestedState
    fun pause() { requested.compareAndSet(null, DownloadState.PAUSED); abort() }
    fun cancel() { requested.set(DownloadState.CANCELED); abort() }
    internal fun abort() { aborted.set(true); connections.forEach(HttpURLConnection::disconnect) }
    internal fun fork() = DownloadControl(this)
    internal fun check() {
        parent?.check()
        if (aborted.get() || requested.get() != null || Thread.currentThread().isInterrupted) throw TransferStopped()
    }
    private fun track(connection: HttpURLConnection) { connections.add(connection); parent?.track(connection) }
    private fun untrack(connection: HttpURLConnection) { connections.remove(connection); parent?.untrack(connection) }
    internal fun attach(connection: HttpURLConnection) {
        track(connection)
        try { check() } catch (error: Exception) { detach(connection); throw error }
    }
    internal fun detach(connection: HttpURLConnection) { untrack(connection); connection.disconnect() }
}
internal class TransferStopped : IOException("下载已停止")
