package moe.pure.browser.feature.sync.network

import moe.pure.browser.feature.sync.model.SyncCall
import okhttp3.Call
import java.io.InterruptedIOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

internal class SyncOperation : SyncCall {
    private val cancelled = AtomicBoolean()
    private val done = AtomicBoolean()
    private val calls = ConcurrentHashMap.newKeySet<Call>()
    var cancelledAction: (() -> Unit)? = null
    @Volatile private var future: Future<*>? = null
    override val isCancelled get() = cancelled.get()
    override val isDone get() = done.get()
    override fun cancel() {
        if (!cancelled.compareAndSet(false, true)) return
        calls.forEach(Call::cancel); calls.clear(); future?.cancel(true); done.set(true)
        cancelledAction?.invoke(); cancelledAction = null
    }
    fun attach(task: Future<*>) { future = task; if (isCancelled) task.cancel(true) }
    fun attach(call: Call) { calls.add(call); if (isCancelled) { call.cancel(); calls.remove(call) } }
    fun check() { if (isCancelled || Thread.currentThread().isInterrupted) throw InterruptedIOException("同步已取消") }
    fun finish() { done.set(true); calls.clear(); future = null; cancelledAction = null }
}
