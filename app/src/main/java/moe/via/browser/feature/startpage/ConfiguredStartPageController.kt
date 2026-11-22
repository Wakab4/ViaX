package moe.via.browser.feature.startpage

import android.content.Context
import android.os.Handler
import android.os.Looper
import moe.via.browser.feature.home.HomeStartPageMatches
import moe.via.browser.feature.startpage.document.ConfiguredStartPageDocuments
import moe.via.browser.browser.session.BrowserTab
import java.lang.ref.WeakReference
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask
import moe.via.browser.core.concurrent.BrowserExecutors

/** A null-source command creates no tab until M0's worker has returned its page model. */
class ConfiguredStartPageController(context: Context, private val host: Host) {
    interface Host {
        fun current(): BrowserTab?
        fun create(url: String, select: Boolean, index: Int): BrowserTab?
        fun navigate(original: BrowserTab, url: String)
        fun failed(error: Throwable)
    }
    private class Pending(val select: Boolean, val index: Int, val original: WeakReference<BrowserTab>?,
        val completed: (() -> Unit)?, val resolved: ((String) -> Unit)? = null, var task: FutureTask<Unit>? = null)
    private val window = context
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val pending = mutableMapOf<Long, Pending>()
    private var next = 0L
    private var disposed = false

    fun create(select: Boolean, index: Int, completed: (() -> Unit)? = null) {
        submit(Pending(select, index, null, completed))
    }

    fun home() {
        if (disposed) return
        val original = host.current() ?: return
        if (HomeStartPageMatches.matches(app, original.liveView?.url ?: original.url)) return
        submit(Pending(false, 0, WeakReference(original), null))
    }

    /** a1/f resolves internal Intent data as a page model, without a provisional default tab. */
    fun resolveInternal(raw: String, resolved: (String) -> Unit): Boolean {
        if (disposed) return false
        val url = moe.via.browser.feature.internalpages.GeneratedAddress.normalize(raw)
        val kind = when {
            moe.via.browser.feature.internalpages.GeneratedAddress.isV(url) -> moe.via.browser.feature.internalpages.GeneratedAddress.kind(url)
            url.startsWith("history://", true) -> 3
            url.startsWith("folder://", true) -> 2
            else -> return false
        }
        val argument = if (url.startsWith("folder://", true)) url.substring(9) else null
        val request = moe.via.browser.feature.internalpages.GeneratedPageRequests.prepare(window, kind, argument, null) ?: return false
        submit(Pending(false, 0, null, null, resolved), request::generate)
        return true
    }

    private fun submit(command: Pending, supplied: ((Context) -> String)? = null) {
        if (disposed) return
        val generate = supplied ?: ConfiguredStartPageDocuments.prepare(window).let { request ->
            { context: Context -> ConfiguredStartPageDocuments.generate(context, request) }
        }
        val id = ++next
        pending[id] = command
        val application = app
        val owner = WeakReference(this)
        val dispatcher = main
        val work = FutureTask<Unit>(Callable {
            val result = runCatching { generate(application) }
            dispatcher.post {
                val controller = owner.get()?.takeUnless { it.disposed } ?: return@post
                val current = controller.pending.remove(id) ?: return@post
                result.onSuccess { url ->
                    if (current.resolved != null) current.resolved.invoke(url)
                    else if (current.original == null) {
                        if (controller.host.create(url, current.select, current.index) != null) current.completed?.invoke()
                    } else current.original.get()?.let { controller.host.navigate(it, url) }
                }.onFailure { controller.host.failed(it) }
            }
            Unit
        })
        command.task = work
        BrowserExecutors.io.execute(work)
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        pending.values.forEach { it.task?.cancel(false) }
        pending.clear()
    }
}
