package moe.via.browser.ui.message

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import moe.via.browser.core.logging.NativeLog as Log
import android.widget.FrameLayout
import java.util.ArrayDeque

/** f6.i: one process slot, one pending add and one timeout. Fading rows are no longer in that slot. */
object ProcessMessageManager {
    private enum class Phase { PENDING, SHOWN, EXITING, REMOVED }
    private class Entry(val owner: Any, val activity: Activity, val parent: FrameLayout, val content: MessageContent) {
        lateinit var renderer: MessageRenderer
        var phase = Phase.PENDING
        var clicked = false
        var cancelled = false
        val afterRemoval = mutableListOf<() -> Unit>()
    }
    private val main = Handler(Looper.getMainLooper())
    private val entries = linkedSetOf<Entry>()
    private var current: Entry? = null
    private var application: Application? = null
    private var mutation = 0
    private val removedCallbacks = ArrayDeque<() -> Unit>()
    private val add = Runnable { batch { addCurrent() } }
    private val expire = Runnable { batch { hideGlobal(true) } }

    internal fun show(owner: Any, activity: Activity, requestedParent: FrameLayout?, content: MessageContent): Boolean {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post { show(owner, activity, requestedParent, content) }; return false }
        val parent = requestedParent ?: activity.findViewById<FrameLayout>(android.R.id.content)
        if (activity.isDestroyed || parent == null || !parent.isShown) return false
        watch(activity.application)
        return batch {
            main.removeCallbacks(add); main.removeCallbacks(expire)
            hideGlobal(false)
            if (content.text.isEmpty()) return@batch false
            val entry = Entry(owner, activity, parent, content)
            entry.renderer = MessageRenderer(activity, content, { click(entry) }, { detached(entry) })
            entries += entry
            current = entry
            // Occupy the Native field before posting message 1, including its pending-add edge case.
            main.post(add)
            true
        }
    }
    private fun addCurrent() {
        val entry = current ?: return
        if (entry.phase != Phase.PENDING || entry.renderer.shown) return
        try {
            entry.parent.addView(entry.renderer.body)
            entry.phase = Phase.SHOWN
            entry.renderer.added()
        } catch (error: Exception) { Log.warn("Messages", "Unable to attach message", error) }
        val delay = entry.content.timeoutMillis ?: if (entry.content.action.isNullOrEmpty()) 1600L else 3500L
        main.postDelayed(expire, delay.coerceAtLeast(0))
    }
    private fun click(entry: Entry) {
        if (entry.phase == Phase.REMOVED) return
        entry.clicked = true
        entry.content.clicked?.invoke()
        // h.l is deliberately global after the callback. It can clear a newly allocated, not-yet-added row.
        batch { hideGlobal(true) }
    }
    private fun hideGlobal(animate: Boolean) {
        val before = current ?: return
        beginRemoval(before, animate)
        current?.let(::cancelNative)
        val displaced = current
        current = null
        // f finally clears its field even if an onCancel callback has replaced it. Drop any orphan pending row.
        if (displaced !== before && displaced != null && displaced.phase == Phase.PENDING) remove(displaced)
    }
    private fun beginRemoval(entry: Entry, animate: Boolean) {
        if (entry.phase == Phase.REMOVED || entry.phase == Phase.EXITING) return
        if (animate && entry.renderer.shown) {
            entry.phase = Phase.EXITING
            entry.renderer.exit { batch { remove(entry) } }
        } else remove(entry)
    }
    private fun cancelled(entry: Entry) {
        if (entry.cancelled || entry.clicked) return
        entry.cancelled = true
        entry.content.cancelled?.invoke()
    }
    private fun cancelNative(entry: Entry) {
        if (entry.clicked) return
        // h.j has no pre-callback once flag: a reentrant f may call it again before the outer f clears its field.
        entry.content.cancelled?.invoke()
        entry.cancelled = true
    }
    private fun remove(entry: Entry) {
        if (entry.phase == Phase.REMOVED) return
        entry.phase = Phase.REMOVED
        entry.renderer.dispose()
        entry.parent.removeView(entry.renderer.body)
        entries.remove(entry)
        entry.content.removed?.let(removedCallbacks::addLast)
        entry.afterRemoval.forEach(removedCallbacks::addLast); entry.afterRemoval.clear()
    }
    private fun detached(entry: Entry) {
        if (entry.phase == Phase.REMOVED) return
        batch {
            if (current === entry) { current = null; main.removeCallbacks(add); main.removeCallbacks(expire) }
            cancelled(entry); remove(entry)
        }
    }
    internal fun close(owner: Any, animate: Boolean, finished: () -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post { close(owner, animate, finished) }; return }
        batch {
            val owned = entries.filter { it.owner === owner }
            if (owned.isEmpty()) { removedCallbacks.addLast(finished); return@batch }
            var remaining = owned.size
            owned.forEach { entry ->
                entry.afterRemoval += { if (--remaining == 0) finished() }
                // A scoped lifecycle close must not clear a different owner allocated by its callback.
                if (current === entry) { current = null; main.removeCallbacks(add); main.removeCallbacks(expire) }
                cancelled(entry)
                if (!animate && entry.phase == Phase.EXITING) remove(entry) else beginRemoval(entry, animate)
            }
        }
    }
    fun release(activity: Activity) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post { release(activity) }; return }
        batch {
            entries.filter { it.activity === activity }.forEach { entry ->
                if (current === entry) { current = null; main.removeCallbacks(add); main.removeCallbacks(expire) }
                cancelled(entry); remove(entry)
            }
        }
    }
    private fun watch(app: Application) {
        if (application === app) return
        application = app
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityDestroyed(activity: Activity) = release(activity)
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        })
    }
    private fun <T> batch(action: () -> T): T {
        mutation++
        try { return action() }
        finally {
            mutation--
            if (mutation == 0) while (removedCallbacks.isNotEmpty()) removedCallbacks.removeFirst().invoke()
        }
    }
}
