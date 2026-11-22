package moe.via.browser.ui.tasks

import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.core.reactive.NativeReactiveErrors

/** u8.b/AndroidLifecycleScopeProvider choose a boundary at each subscription, not at view creation. */
internal class NativeLifecycleJobs(private val lifecycle: () -> Lifecycle) : AutoCloseable {
    private class Subscription(val lifecycle: Lifecycle, val jobs: NativeViewJobs = NativeViewJobs()) {
        var observer: LifecycleEventObserver? = null
        var closed = false
    }
    private val subscriptions = HashSet<Subscription>()
    private var closed = false
    fun <T> launch(work: () -> T, until: Lifecycle.Event? = null,
                   failed: (Throwable) -> Unit = { NativeLog.warn(null, it) }, delivered: (T) -> Unit) {
        val subscription = bind(until, failed) ?: return
        execute(subscription, work, failed, delivered)
    }
    /** Maybe.fromCallable completes with no success callback when its callable returns null. */
    fun <T : Any> maybe(work: () -> T?, until: Lifecycle.Event? = null,
                        failed: (Throwable) -> Unit = { NativeLog.warn(null, it) }, delivered: (T) -> Unit) {
        launch(work, until, failed) { value -> if (value != null) delivered(value) }
    }
    /** r8.d's two one-item Observables share one default scope and one onNext/error boundary. */
    fun <A, B> zip(first: () -> A, second: () -> B,
                   failed: (Throwable) -> Unit = { NativeLog.warn(null, it) }, delivered: (A, B) -> Unit) {
        val subscription = bind(null, failed) ?: return
        subscription.jobs.zip(first, second, failed = { error -> if (finish(subscription)) failed(error) }) { a, b ->
            if (!subscription.closed) {
                NativeReactiveErrors.next({ delivered(a, b) }) { error -> finish(subscription); failed(error) }
                finish(subscription)
            }
        }
    }
    /** Maybe.fromCallable + observeOn(IO) + observeOn(main) keeps source work on the caller. */
    fun <T> fromCaller(work: () -> T, until: Lifecycle.Event? = null,
                       failed: (Throwable) -> Unit = { NativeLog.warn(null, it) }, delivered: (T) -> Unit) {
        val subscription = bind(until, failed) ?: return
        val value = try { Result.success(work()) } catch (error: Throwable) { NativeReactiveErrors.throwIfFatal(error); Result.failure(error) }
        execute(subscription, { value.getOrThrow() }, failed, delivered)
    }
    private fun <T> execute(subscription: Subscription, work: () -> T, failed: (Throwable) -> Unit, delivered: (T) -> Unit) {
        subscription.jobs.launch(work, failed = { error -> if (finish(subscription)) failed(error) }) { value ->
            if (finish(subscription)) delivered(value)
        }
    }
    private fun bind(until: Lifecycle.Event?, failed: (Throwable) -> Unit): Subscription? {
        if (closed) return null
        if (Looper.myLooper() != Looper.getMainLooper()) {
            NativeReactiveErrors.failure(IllegalStateException("Lifecycles can only be bound to on the main thread!"), failed); return null
        }
        val source = lifecycle()
        val peek = when (source.currentState) {
            Lifecycle.State.INITIALIZED -> Lifecycle.Event.ON_CREATE
            Lifecycle.State.CREATED -> Lifecycle.Event.ON_START
            Lifecycle.State.STARTED, Lifecycle.State.RESUMED -> Lifecycle.Event.ON_RESUME
            Lifecycle.State.DESTROYED -> Lifecycle.Event.ON_DESTROY
        }
        val end = until ?: when (peek) {
            Lifecycle.Event.ON_CREATE -> Lifecycle.Event.ON_DESTROY
            Lifecycle.Event.ON_START, Lifecycle.Event.ON_PAUSE -> Lifecycle.Event.ON_STOP
            Lifecycle.Event.ON_RESUME -> Lifecycle.Event.ON_PAUSE
            else -> { NativeReactiveErrors.failure(IllegalStateException("Lifecycle has ended! Last event was $peek"), failed); return null }
        }
        val subscription = Subscription(source)
        var first = true
        val observer = LifecycleEventObserver { _, event ->
            if (first) first = false else if (event.ordinal >= end.ordinal) finish(subscription)
        }
        subscription.observer = observer
        subscriptions.add(subscription)
        source.addObserver(observer)
        return subscription.takeUnless { it.closed }
    }
    private fun finish(subscription: Subscription): Boolean {
        if (subscription.closed) return false
        subscription.closed = true
        subscription.observer?.let(subscription.lifecycle::removeObserver)
        subscriptions.remove(subscription); subscription.jobs.close(); return true
    }
    override fun close() { closed = true; subscriptions.toList().forEach { finish(it) } }
}
