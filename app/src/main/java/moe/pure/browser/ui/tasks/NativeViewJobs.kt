package moe.pure.browser.ui.tasks

import android.os.Handler
import android.os.Looper
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.core.reactive.NativeReactiveErrors
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

/** Cached IO, native consumer boundaries and async main delivery belong to one disposable owner. */
internal class NativeViewJobs : AutoCloseable {
    private val main = Handler.createAsync(Looper.getMainLooper())
    private val jobs = HashSet<Job>()
    private val children = HashSet<NativeViewJobs>()
    @Volatile private var closed = false
    private inner class Job(work: () -> Unit) : FutureTask<Unit>(Callable {
        try { work() } catch (error: Throwable) { NativeReactiveErrors.report(error); throw error }
        Unit
    }) {
        @Volatile var runner: Thread? = null
        override fun run() { runner = Thread.currentThread(); try { super.run() } finally { runner = null } }
        override fun done() { synchronized(jobs) { jobs.remove(this) } }
    }
    private fun submit(work: () -> Unit) {
        val future = Job(work)
        synchronized(jobs) { if (closed) return; jobs.add(future) }
        BrowserExecutors.io.execute(future)
    }
    fun <T> launch(work: () -> T, failed: (Throwable) -> Unit = { NativeLog.warn(null, it) }, delivered: (T) -> Unit) {
        submit {
            try {
                val value = work()
                post { NativeReactiveErrors.success { delivered(value) } }
            } catch (error: Throwable) {
                NativeReactiveErrors.throwIfFatal(error)
                if (closed) NativeReactiveErrors.report(error) else post { NativeReactiveErrors.failure(error, failed) }
            }
        }
    }
    private fun post(delivered: () -> Unit) {
        synchronized(jobs) {
            if (closed) return
            main.post { if (!closed) try { delivered() } catch (error: Throwable) { NativeReactiveErrors.report(error) } }
        }
    }
    /** r8.d combines two one-item Observable callables on IO, then observes their result on main. */
    fun <A, B> zip(first: () -> A, second: () -> B,
                   failed: (Throwable) -> Unit = { NativeLog.warn(null, it) }, delivered: (A, B) -> Unit) {
        val child = NativeViewJobs()
        synchronized(jobs) { if (closed) { child.close(); return }; children.add(child) }
        val gate = Any()
        var left: Result<A>? = null
        var right: Result<B>? = null
        var stopped = false
        fun finish() { synchronized(jobs) { children.remove(child) }; child.close() }
        fun fail(error: Throwable) {
            val accepted = synchronized(gate) { if (stopped) false else { stopped = true; true } }
            if (accepted) { finish(); post { NativeReactiveErrors.failure(error, failed) } }
        }
        fun completed(a: A, b: B) { finish(); post { NativeReactiveErrors.next({ delivered(a, b) }, failed) } }
        fun <T> source(work: () -> T, value: (T) -> Unit) {
            if (child.closed) return
            try {
                val result = work()
                if (result == null) throw NullPointerException("Callable returned a null value. Null values are generally not allowed in 3.x operators and sources.")
                if (!child.closed) value(result)
            } catch (error: Throwable) {
                NativeReactiveErrors.throwIfFatal(error)
                if (child.closed) NativeReactiveErrors.report(error) else fail(error)
            }
        }
        child.submit {
            source(first) { a ->
                val pair = synchronized(gate) {
                    if (stopped) null else {
                        left = Result.success(a)
                        right?.let { b -> stopped = true; a to b.getOrThrow() }
                    }
                }
                pair?.let { completed(it.first, it.second) }
            }
        }
        if (!synchronized(gate) { stopped }) child.submit {
            source(second) { b ->
                val pair = synchronized(gate) {
                    if (stopped) null else {
                        right = Result.success(b)
                        left?.let { a -> stopped = true; a.getOrThrow() to b }
                    }
                }
                pair?.let { completed(it.first, it.second) }
            }
        }
    }
    override fun close() {
        val pending: List<Job>
        val nested: List<NativeViewJobs>
        synchronized(jobs) {
            closed = true; pending = jobs.toList(); jobs.clear()
            nested = children.toList(); children.clear()
        }
        nested.forEach(NativeViewJobs::close)
        val current = Thread.currentThread()
        pending.forEach { it.cancel(it.runner !== current) }
        main.removeCallbacksAndMessages(null)
    }
}
