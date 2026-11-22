package moe.via.browser.feature.ai.network.sse

import java.io.EOFException
import java.io.IOException
import java.util.Random
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.ai.network.AiCall
import moe.via.browser.feature.ai.network.AiException
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

/** The original EventSource has separate ordered stream/event executors and default EOF reconnect. */
internal class NativeAiEventSource(base: OkHttpClient, private val request: Request, private val handler: Handler) : AiCall {
    enum class State { RAW, CONNECTING, OPEN, CLOSED, SHUTDOWN }
    interface Handler {
        fun opened() {}
        fun closed()
        fun comment(value: String) {}
        fun event(type: String, value: String)
        fun error(error: Throwable)
    }
    private val client = base.newBuilder().build()
    private val ready = AtomicReference(State.RAW)
    private val events = Executors.newSingleThreadExecutor(factory("okhttp-eventsource-events"))
    private val stream = Executors.newSingleThreadExecutor(factory("okhttp-eventsource-stream"))
    private val random = Random()
    @Volatile private var retryMillis = 1000L
    @Volatile private var lastEventId: String? = null
    @Volatile private var call: Call? = null
    val state: State get() = ready.get()
    override val isCancelled: Boolean get() = state == State.SHUTDOWN
    fun start() { if (ready.compareAndSet(State.RAW, State.CONNECTING)) stream.execute(::run) }
    private fun run() {
        var attempt = 0
        var connectedAt = 0L
        while (!Thread.currentThread().isInterrupted && state != State.SHUTDOWN) {
            try {
                if (attempt == 0) attempt++ else {
                    if (retryMillis > 0L) {
                        if (connectedAt > 0 && System.currentTimeMillis() - connectedAt >= 60000L) attempt = 1
                        val exponent = if (attempt < 31) 1 shl attempt else Int.MAX_VALUE
                        val millis = minOf(30000L, retryMillis * exponent.toLong())
                        val bound = if (millis > Int.MAX_VALUE) Int.MAX_VALUE else millis.toInt()
                        try { Thread.sleep((bound / 2 + random.nextInt(bound) / 2).toLong()) } catch (_: InterruptedException) { }
                        attempt++
                    }
                }
                connectedAt = connect()
            } catch (_: RejectedExecutionException) { call = null; return }
        }
    }
    private fun connect(): Long {
        ready.set(State.CONNECTING)
        var connectedAt = 0L
        val next = request.newBuilder().apply {
            if (request.header("Accept") == null) addHeader("Accept", "text/event-stream")
            if (request.header("Cache-Control") == null) addHeader("Cache-Control", "no-cache")
            lastEventId?.takeIf(String::isNotEmpty)?.let { addHeader("Last-Event-ID", it) }
        }.build()
        call = client.newCall(next)
        try {
            call!!.execute().use { response ->
                if (response.isSuccessful) {
                    connectedAt = System.currentTimeMillis(); ready.set(State.OPEN); emit { handler.opened() }
                    val parser = AiSseFrames({ type, data -> emit { handler.event(type, data) } },
                        { value -> emit { handler.comment(value) } }, { lastEventId = it }, { retryMillis = it })
                    try {
                        response.body.source().use { source ->
                            while (!Thread.currentThread().isInterrupted && !source.exhausted()) parser.line(source.readUtf8LineStrict())
                        }
                    } catch (_: EOFException) { }
                } else emitError(AiException(100, httpCode = response.code, responseBody = response.body.string()))
            }
        } catch (error: IOException) { if (state != State.SHUTDOWN && state != State.CLOSED) emitError(error) }
        finally {
            if (ready.compareAndSet(State.OPEN, State.CLOSED)) emit { handler.closed() }
            else ready.compareAndSet(State.CONNECTING, State.CLOSED)
        }
        return connectedAt
    }
    private fun emit(work: () -> Unit) { events.execute {
        try { work() } catch (error: Exception) { NativeLog.warn(null, error); deliverError(error) }
    } }
    private fun emitError(error: Throwable) = events.execute { deliverError(error) }
    private fun deliverError(error: Throwable) { try { handler.error(error) } catch (nested: Throwable) { NativeLog.warn(null, nested) } }
    override fun cancel() {
        val before = ready.getAndSet(State.SHUTDOWN)
        if (before == State.SHUTDOWN) return
        if (before == State.OPEN) emit { handler.closed() }
        call?.cancel(); events.shutdown(); stream.shutdown()
        client.connectionPool.evictAll(); client.dispatcher.cancelAll(); client.dispatcher.executorService.shutdownNow()
    }
    private fun factory(name: String): java.util.concurrent.ThreadFactory {
        val count = AtomicLong()
        return java.util.concurrent.ThreadFactory { work -> Thread(work, "$name-[]-${count.getAndIncrement()}").apply { isDaemon = true; priority = Thread.NORM_PRIORITY } }
    }
}
