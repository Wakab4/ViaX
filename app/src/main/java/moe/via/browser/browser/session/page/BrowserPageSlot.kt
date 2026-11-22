package moe.via.browser.browser.session.page

import android.os.Bundle
import moe.via.browser.browser.session.state.TabPageIdentity
import moe.via.browser.webview.selection.BrowserPageView
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicInteger

/** A t4.c node survives while its WebView may be saved and destroyed independently. UI-thread owner. */
internal class BrowserPageSlot(initialUrl: String? = null, initialState: Bundle = Bundle()) {
    val id = IDS.getAndIncrement()
    val identity = TabPageIdentity.restore(initialState)
    @Volatile var logicalUrl: String? = initialUrl ?: initialState.getString("url")
    @Volatile var title: String? = null
    var view: BrowserPageView? = null
    var state = Bundle(initialState)
    var lastInactiveUptime = 0L
    var mediaPaused = false
    var popupFirst = false
    var referer: String? = null
    var sendReferer = false
    var deferredLoad: Runnable? = null
    private var scrollTask: Runnable? = null

    fun capture(): Bundle {
        val source = view ?: return savedCopy()
        val next = Bundle()
        try { source.saveState(next) } catch (_: RuntimeException) { return savedCopy() }
        next.putString(TabPageIdentity.KEY, identity)
        val address = logicalUrl ?: source.url
        if (!address.isNullOrEmpty()) next.putString("url", address)
        val scroll = source.scrollX.toLong() or (source.scrollY.toLong() shl 32)
        if (scroll != 0L) next.putLong("scroll", scroll)
        state = next
        return Bundle(next)
    }

    private fun savedCopy() = Bundle(state).apply { putString(TabPageIdentity.KEY, identity) }

    fun restoreScroll(source: BrowserPageView, loadedFallback: Boolean) {
        val scroll = state.getLong("scroll", 0)
        if (scroll == 0L) return
        val reference = WeakReference(source)
        val task = Runnable {
            val target = reference.get()
            if (target != null && view === target && target.scrollY <= 1000)
                target.scrollTo(scroll.toInt(), (scroll ushr 32).toInt())
        }
        scrollTask = task
        source.postDelayed(task, if (loadedFallback) 500 else 100)
    }

    fun cancelCallbacks() {
        scrollTask?.let { view?.removeCallbacks(it) }
        scrollTask = null
        deferredLoad?.let { view?.removeCallbacks(it) }
        deferredLoad = null
    }

    companion object {
        private val IDS = AtomicInteger(1)
    }
}
