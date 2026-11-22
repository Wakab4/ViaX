package moe.via.browser.feature.library.history.document

import android.content.Context
import android.os.Handler
import android.os.Looper
import moe.via.browser.core.logging.NativeLog as Log
import moe.via.browser.feature.internalpages.BrowserInternalPages
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.library.history.HistoryDirtyState
import moe.via.browser.browser.session.BrowserTab
import java.lang.ref.WeakReference

/** ua.s1/h and x1/q: IO renders values; completion resolves a live window or a weak original tab. */
class HistoryDocumentController(context: Context, private val host: Host) {
    interface Host {
        fun current(): BrowserTab?
        fun options(): HistoryDocumentOptions
        fun navigate(tab: BrowserTab, url: String)
        fun append(url: String)
    }

    private val app = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var disposed = false

    fun open() {
        if (disposed) return
        val options = host.options()
        val application = app
        val owner = WeakReference(this)
        val dispatcher = main
        BrowserExecutors.io.execute {
            val result = runCatching { HistoryDocument(application).generate(workerOptions(application, options)) }
            dispatcher.post {
                val controller = owner.get()?.takeUnless { it.disposed } ?: return@post
                result.onSuccess(controller::opened).onFailure { Log.warn("HistoryDocument", "Open history", it) }
            }
        }
    }

    fun refreshCurrent() { host.current()?.let(::refreshIfDirty) }

    fun refreshIfDirty(tab: BrowserTab) {
        if (disposed || !HistoryDirtyState.isDirty() || !HistoryDocument.isPage(app, tab.liveView?.url ?: tab.url)) return
        val options = host.options()
        val application = app
        val owner = WeakReference(this)
        val original = WeakReference(tab)
        val dispatcher = main
        BrowserExecutors.io.execute {
            val result = runCatching { HistoryDocument(application).generate(workerOptions(application, options)) }
            dispatcher.post {
                val controller = owner.get()?.takeUnless { it.disposed } ?: return@post
                val page = original.get() ?: return@post
                // Native q navigates the original weak tab, without a later URL/selection/epoch filter.
                result.onSuccess { controller.host.navigate(page, it) }
                    .onFailure { Log.warn("HistoryDocument", "Refresh history", it) }
            }
        }
    }

    fun delete(url: String) {
        if (disposed) return
        HistoryDocumentEdits.delete(app, url)
        refreshCurrent()
    }

    /** Called only after the context-menu owner has accepted Native's clear confirmation. */
    fun clear() {
        if (disposed) return
        HistoryDocumentEdits.clear(app)
        refreshCurrent()
    }

    fun dispose() { disposed = true }

    private fun opened(url: String) {
        // ua.h asks again after IO: generated internal pages are reused; others append at all-tabs end.
        val tab = host.current()
        if (tab != null && BrowserInternalPages.kind(app, tab.liveView?.url ?: tab.url) > 0) host.navigate(tab, url)
        else host.append(url)
    }

    private companion object {
        fun workerOptions(context: Context, options: HistoryDocumentOptions): HistoryDocumentOptions {
            val style = moe.via.browser.feature.internalpages.render.GeneratedPageInputs.style(context, options.isRtl)
            val preferences = moe.via.browser.storage.preferences.BrowserPreferences(context.getSharedPreferences("browser", Context.MODE_PRIVATE))
            return options.copy(isNight = style.night, isLight = style.light, globalIncognito = preferences.incognito)
        }
    }
}
