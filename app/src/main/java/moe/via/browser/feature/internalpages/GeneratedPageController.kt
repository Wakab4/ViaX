package moe.via.browser.feature.internalpages

import android.content.Context
import android.os.Handler
import android.os.Looper
import moe.via.browser.core.logging.NativeLog as Log
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.browser.session.BrowserTab
import java.lang.ref.WeakReference

/** ua.s1/h and x1/q share rendering, but use different navigation targets on completion. */
class GeneratedPageController(context: Context, private val host: Host) {
    interface Host {
        fun current(): BrowserTab?
        fun navigate(tab: BrowserTab, url: String)
        fun append(url: String, adjacent: Boolean)
        fun networkSource(): GeneratedNetworkSource
    }

    private val window = context
    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var disposed = false

    fun open(kind: Int, argument: String? = null, network: GeneratedNetworkSource? = null) {
        if (disposed) return
        val request = GeneratedPageRequests.prepare(window, kind, argument,
            if (kind == 6 || kind == 10) network ?: host.networkSource() else null) ?: return
        val application = app
        val owner = WeakReference(this)
        val dispatcher = main
        BrowserExecutors.io.execute {
            val result = runCatching { request.generate(application) }
            dispatcher.post {
                val controller = owner.get()?.takeUnless { it.disposed } ?: return@post
                result.onSuccess { controller.opened(kind, it) }
                    .onFailure { Log.warn("GeneratedPage", "Open generated document", it) }
            }
        }
    }
    fun openNetwork(resources: Boolean, source: GeneratedNetworkSource) = open(if (resources) 10 else 6, network = source)

    fun refreshCurrent() { host.current()?.let(::refreshIfDirty) }

    fun refreshIfDirty(tab: BrowserTab) {
        if (disposed) return
        val url = tab.liveView?.url ?: tab.url
        val kind = BrowserInternalPages.kind(app, url)
        if (kind == 1 && !GeneratedPageDirtyState.isDirty(GeneratedPageDirtyState.HOME) &&
            !GeneratedPageDirtyState.isDirty(GeneratedPageDirtyState.HOME_STYLE)) return
        val bit = when (kind) {
            1 -> 0
            2, 11 -> GeneratedPageDirtyState.BOOKMARKS
            4 -> GeneratedPageDirtyState.ABOUT
            5 -> GeneratedPageDirtyState.CATALOG
            // ua.x1 does not rebuild saved/network pages merely because they are selected.
            else -> return
        }
        if (bit != 0 && !GeneratedPageDirtyState.isDirty(bit)) return
        val argument = if (kind == 11) folderArgument(url) else null
        val request = GeneratedPageRequests.prepare(window, kind, argument, null) ?: return
        val application = app
        val owner = WeakReference(this)
        val original = WeakReference(tab)
        val dispatcher = main
        BrowserExecutors.io.execute {
            val result = runCatching { request.generate(application) }
            dispatcher.post {
                val controller = owner.get()?.takeUnless { it.disposed } ?: return@post
                val page = original.get() ?: return@post
                result.onSuccess { controller.host.navigate(page, it) }
                    .onFailure { Log.warn("GeneratedPage", "Refresh generated document", it) }
            }
        }
    }

    fun dispose() { disposed = true }

    private fun opened(kind: Int, url: String) {
        val current = host.current()
        if (current != null && BrowserInternalPages.kind(app, current.liveView?.url ?: current.url) > 0) host.navigate(current, url)
        else host.append(url, kind == 6 || kind == 10)
    }

    private fun folderArgument(url: String): String? {
        val index = url.lastIndexOf("?folder=")
        return if (index > 0) url.substring(index + 8).trim { it <= ' ' } else null
    }
}
