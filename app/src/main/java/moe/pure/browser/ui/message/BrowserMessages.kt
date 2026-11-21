package moe.pure.browser.ui.message

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import moe.pure.browser.R
import java.lang.ref.WeakReference

/** Text-only application notices share f6.i's existing process manager and never use platform Toast. */
object BrowserMessages {
    const val SHORT = 1600L
    const val LONG = 3500L
    private data class Pending(val text: String, val duration: Long, val target: WeakReference<Activity>?)
    private val main = Handler(Looper.getMainLooper())
    private var application: Application? = null
    private var foreground: WeakReference<Host>? = null
    private var pending: Pending? = null

    fun show(context: Context, text: CharSequence, durationMillis: Long = SHORT) {
        // Resolve resources/spans now; a queued request must not retain an arbitrary Context or span.
        val request = Pending(text.toString(), durationMillis.coerceAtLeast(0), activity(context)?.let(::WeakReference))
        val app = context.applicationContext as? Application
        if (Looper.myLooper() == Looper.getMainLooper()) { app?.let(::watch); deliver(request) }
        else main.post { app?.let(::watch); deliver(request) }
    }
    fun show(context: Context, text: Int, durationMillis: Long = SHORT) =
        show(context, context.getText(text).toString(), durationMillis)

    /** Called once for a window; the decor owns the handle and the global pointer is weak. */
    fun bind(activity: Activity) {
        check(Looper.myLooper() == Looper.getMainLooper())
        watch(activity.application)
        host(activity)
    }

    fun resumed(activity: Activity) {
        if (activity.isDestroyed || activity.isFinishing) return
        val owner = host(activity)
        owner.resumed = true
        foreground = WeakReference(owner)
        val reference = WeakReference(owner)
        activity.window.decorView.post {
            val live = reference.get()?.takeIf { it.resumed && !it.closed } ?: return@post
            if (foreground?.get() === live) pending?.let(::deliver)
        }
    }
    fun paused(activity: Activity) {
        existing(activity)?.let { owner ->
            owner.resumed = false
            if (foreground?.get() === owner) foreground = null
        }
    }
    fun destroyed(activity: Activity) {
        val owner = existing(activity)
        owner?.close()
        if (foreground?.get() === owner) foreground = null
        if (pending?.target?.get() === activity) pending = null
        activity.window.decorView.setTag(R.id.browser_message_host, null)
    }

    private class Host(val activity: Activity) {
        val messages = MessageOwner(activity)
        var resumed = false
        var closed = false
        fun close() {
            if (closed) return
            closed = true; resumed = false
            messages.close(false)
        }
    }
    private fun existing(activity: Activity) = activity.window.decorView.getTag(R.id.browser_message_host) as? Host
    private fun host(activity: Activity) = existing(activity) ?: Host(activity).also {
        activity.window.decorView.setTag(R.id.browser_message_host, it)
    }
    private fun deliver(request: Pending) {
        val target = request.target?.get()
        if (request.target != null && (target == null || target.isDestroyed || target.isFinishing)) {
            if (pending === request) pending = null
            return
        }
        var owner = if (target == null) foreground?.get() else existing(target)
        // A standalone non-Themed Activity may register the bridge after its resume callback.
        if (owner == null && target != null && target.hasWindowFocus() && target.window.decorView.isShown) {
            bind(target); resumed(target); owner = existing(target)
        }
        if (owner == null || owner.closed || !owner.resumed) { pending = request; return }
        if (owner.messages.show(MessageContent(request.text, timeoutMillis = request.duration))) pending = null
        else pending = request.takeIf { it.text.isNotEmpty() }
    }
    private fun activity(context: Context): Activity? {
        var current = context
        val seen = HashSet<Context>()
        while (seen.add(current)) {
            if (current is Activity) return current
            current = (current as? ContextWrapper)?.baseContext ?: return null
        }
        return null
    }

    private fun watch(app: Application) {
        if (application === app) return
        application = app
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = resumed(activity)
            override fun onActivityPaused(activity: Activity) = paused(activity)
            override fun onActivityDestroyed(activity: Activity) = destroyed(activity)
            override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        })
    }
}
