package moe.via.browser.browser.session.page

import android.os.Bundle
import android.os.SystemClock
import android.webkit.URLUtil
import android.webkit.WebView
import moe.via.browser.browser.session.state.TabNativeStateCodec
import moe.via.browser.webview.selection.BrowserPageView
import java.util.concurrent.ConcurrentHashMap

/** r4.d's runtime page list. Each node owns an independent engine history and may be hibernated. */
internal class BrowserPageStack(private val initialUrl: String, initialOuter: ByteArray?,
    private val policy: PageStackNavigationPolicy, private val host: Host) {
    interface Host {
        fun create(pageId: Int): BrowserPageView
        fun prepare(view: WebView, url: String?)
        fun changed(previous: WebView?, current: WebView?, flags: Int)
        fun metadata(url: String?, title: String?, progress: Int)
        fun engineUrl(url: String): String
        fun logicalUrl(url: String, previousUrl: String?): String
    }

    private val slots = mutableListOf<BrowserPageSlot>()
    private val views = ConcurrentHashMap<WebView, BrowserPageSlot>()
    private var dormantOuter = initialOuter
    private var index = -1
    @Volatile private var current: BrowserPageSlot? = null
    @Volatile private var disposed = false
    private var selected = false
    private var popupReferer: String? = null
    private var popupPending = false
    val liveView: BrowserPageView? get() = current?.view
    fun owns(view: WebView) = !disposed && views.containsKey(view)
    fun isCurrent(view: WebView) = !disposed && views[view] === current && current?.view === view
    fun pageId(view: WebView): Int? = views[view]?.id
    fun sourceIdentity(view: WebView): String? = views[view]?.identity
    fun liveSource(identity: String): WebView? = views.entries.firstOrNull { it.value.identity == identity }?.key
    fun sourceUrl(view: WebView): String? = views[view]?.logicalUrl

    fun requireCurrent(): BrowserPageView {
        check(!disposed) { "The browser tab has been disposed" }
        initialize()
        return ensureView(checkNotNull(current))
    }

    fun activate() {
        if (disposed) return
        if (dormantOuter == null && slots.isEmpty()) initialize()
        selected = true
        val live = current?.let(::ensureView)
        live?.onResume()
        host.changed(live, live, 9)
        if (dormantOuter != null) initialize()
    }

    fun deactivate() {
        if (disposed) return
        if (dormantOuter == null && slots.isEmpty()) initialize()
        selected = false
        pauseCurrent()
    }

    fun pause() { if (!disposed) { pauseCurrent(); liveView?.pauseTimers() } }
    fun resume() {
        if (disposed) return
        liveView?.onResume()
        if (selected) host.changed(liveView, liveView, 9)
        liveView?.resumeTimers()
    }

    fun navigate(address: String) {
        if (disposed || address.isEmpty()) return
        initialize()
        val source = liveView
        val previousUrl = current?.logicalUrl ?: source?.url
        if (address == previousUrl) {
            if (source == null) push(address, false)
            else { host.prepare(source, address); source.reload() }
            return
        }
        if (URLUtil.isJavaScriptUrl(address)) { source?.loadUrl(address); return }
        val decision = if (source != null && !previousUrl.isNullOrEmpty())
            policy.decide(previousUrl, address, automatic = false, popupFirst = false) else null
        if (source == null || decision != null && decision.code != 2) {
            push(address, inheritReferer = false)
        } else {
            val slot = checkNotNull(current)
            slot.logicalUrl = address
            host.prepare(source, address)
            load(slot, source, address)
            source.markForwardBoundary()
            discardForward()
            publish(slot)
        }
    }

    /** Native p4.k.e consumes the policy result without adding a main-frame or gesture filter. */
    fun shouldOverride(source: WebView, address: String, automatic: Boolean): Boolean {
        beforeNavigation(source, address)?.let { return it }
        val slot = views[source] ?: return false
        if (disposed) return false
        val decision = policy.decide(slot.logicalUrl ?: source.url, address, automatic, popupFirst = false)
        return applyDecision(source, slot, address, decision)
    }

    /** Popup once runs before the ordinary external/internal URL router, after the redirect guard. */
    fun beforeNavigation(source: WebView, address: String): Boolean? {
        val slot = views[source] ?: return null
        if (disposed || !slot.popupFirst) return null
        slot.popupFirst = false
        if (!URLUtil.isNetworkUrl(address)) return null
        return applyDecision(source, slot, address,
            policy.decide(slot.logicalUrl ?: source.url, address, automatic = false, popupFirst = true))
    }

    private fun applyDecision(source: WebView, slot: BrowserPageSlot, address: String,
        decision: PageStackDecision): Boolean {
        if (decision.delayedReload) {
            val task = Runnable {
                slot.deferredLoad = null
                if (!owns(source)) return@Runnable
                source.stopLoading()
                slot.logicalUrl = address
                host.prepare(source, decision.settingsUrl)
                slot.sendReferer = slot.referer != null
                load(slot, source, address)
            }
            slot.deferredLoad?.let(source::removeCallbacks)
            slot.deferredLoad = task
            source.postDelayed(task, 20)
            return true
        }
        return when (decision.code) {
            3 -> true
            1 -> { push(address, inheritReferer = true); true }
            2 -> {
                if (decision.prepare) host.prepare(source, decision.settingsUrl)
                (source as? BrowserPageView)?.clearForwardBoundary()
                discardForward()
                false
            }
            else -> false
        }
    }

    fun preparePopup(referer: String?) {
        popupReferer = referer?.takeIf(URLUtil::isNetworkUrl)
        popupPending = current == null
        current?.let { it.popupFirst = true; it.referer = popupReferer; it.sendReferer = popupReferer != null }
    }

    fun canGoBack() = index > 0 || liveView?.canGoBack() == true
    fun canGoForward() = slots.size > index + 1 || liveView?.canGoForward() == true
    fun goBack() = travel(true)
    fun goForward() = travel(false)
    fun reload() { liveView?.reload() }
    fun stop() { liveView?.stopLoading() }

    fun urls(): List<String?> = slots.map { it.logicalUrl ?: it.state.getString("url") }

    fun reusePage(position: Int): Boolean {
        if (position !in slots.indices) return false
        if (position == index) return true
        if (index > position) index--
        val next = index + 1
        slots.add(next, slots.removeAt(position))
        switchTo(next)
        return true
    }

    fun pageStarted(source: WebView, url: String) {
        val slot = views[source] ?: return
        slot.logicalUrl = url
        if (slot === current) publish(slot)
    }
    fun pageFinished(source: WebView, url: String) = pageStarted(source, url)
    fun titleChanged(source: WebView, title: String?) {
        val slot = views[source] ?: return
        slot.title = title
        if (slot === current) publish(slot)
    }
    fun progressChanged(source: WebView, progress: Int) {
        val slot = views[source] ?: return
        if (slot === current) host.metadata(slot.logicalUrl, slot.title ?: source.title, progress)
    }

    fun snapshot(title: String, url: String): ByteArray {
        dormantOuter?.let { return it }
        if (slots.isEmpty()) return TabNativeStateCodec.metadata(url, title)
        val first = maxOf(0, index - 6)
        val end = minOf(slots.size, index + 4)
        return TabNativeStateCodec.wrapPages(slots.subList(first, end).map { it.capture() }, index - first, title, url)
    }

    fun trimMemory(keep: Int, title: String, url: String) {
        if (disposed || keep < 0 || keep >= slots.size) return
        if (keep == 0) {
            if (selected) sweep(0, 0) else release(title, url)
        } else {
            val rest = keep - 1
            val before = (rest + 1) / 2
            sweep(before, rest - before)
        }
    }

    fun release(title: String, url: String) {
        if (disposed || dormantOuter != null) return
        dormantOuter = snapshot(title, url)
        slots.toList().forEach { releaseView(it, save = false) }
        slots.clear(); index = -1; current = null
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        // Keep current identity visible to the release callback until its actual view is destroyed.
        slots.toList().forEach { releaseView(it, save = false) }
        views.clear(); slots.clear(); current = null; dormantOuter = null; index = -1
    }

    private fun initialize() {
        if (slots.isNotEmpty() || disposed) return
        val outer = dormantOuter
        dormantOuter = null
        val restored = outer?.let(TabNativeStateCodec::pageList)
        if (restored != null && restored.pages.isNotEmpty()) {
            restored.pages.forEach { slots.add(BrowserPageSlot(initialState = it)) }
            switchTo(restored.current.coerceIn(slots.indices))
        } else push(outer?.let(TabNativeStateCodec::decode)?.getString("URL") ?: initialUrl, inheritReferer = false)
    }

    private fun push(address: String, inheritReferer: Boolean) {
        val previous = liveView
        val referer = if (inheritReferer) {
            previous?.markForwardBoundary()
            (current?.logicalUrl ?: previous?.url)?.takeIf(URLUtil::isNetworkUrl)
        } else null
        discardForward()
        val slot = BrowserPageSlot(address).apply {
            this.referer = referer; sendReferer = referer != null
            if (popupPending) { popupFirst = true; this.referer = popupReferer; sendReferer = popupReferer != null; popupPending = false }
        }
        slots.add(slot)
        val next = slots.lastIndex
        val view = ensureView(slot)
        previous?.stopLoading()
        val trimmed = address.trim { it <= ' ' }
        if (trimmed.isEmpty()) host.prepare(view, null)
        else { slot.logicalUrl = trimmed; host.prepare(view, trimmed); load(slot, view, trimmed) }
        switchTo(next)
    }

    private fun switchTo(position: Int) {
        val previous = current
        val oldView = previous?.view
        val backwards = position < index
        previous?.lastInactiveUptime = SystemClock.elapsedRealtime()
        if (oldView != null) {
            if (oldView.progress < 100) oldView.stopLoading()
            runCatching { PageViewLifecycle.pauseMedia(oldView) }
            previous?.mediaPaused = true
            oldView.onPause()
        }
        index = position
        val slot = slots[position]
        current = slot
        val view = ensureView(slot)
        view.onResume()
        if (slot.mediaPaused) { runCatching { PageViewLifecycle.resumeMedia(view) }; slot.mediaPaused = false }
        sweep(3, 2)
        publish(slot)
        host.changed(oldView, view, (if (backwards) 2 else 4) or (if (selected) 1 else 0))
    }

    private fun travel(backwards: Boolean) {
        if (disposed) return
        val source = liveView ?: return
        if (backwards && source.canGoBack()) source.goBack()
        else if (!backwards && source.canGoForward()) source.goForward()
        else (index + if (backwards) -1 else 1).takeIf { it in slots.indices }?.let(::switchTo)
    }

    private fun ensureView(slot: BrowserPageSlot): BrowserPageView {
        slot.view?.let { return it }
        val view = host.create(slot.id)
        slot.view = view
        views[view] = slot
        view.id = slot.id
        val state = slot.state
        if (state.isEmpty) { host.prepare(view, null); return view }
        val restored = runCatching { view.restoreState(state) }.getOrNull()
        var address = restored?.currentItem?.url?.takeIf { it.isNotEmpty() }
        var fallback = false
        if (address == null) { address = state.getString("url")?.takeIf { it.isNotEmpty() }; fallback = address != null }
        val logical = address?.let { host.logicalUrl(it, slot.logicalUrl) }
        if (logical != null) slot.logicalUrl = logical
        host.prepare(view, logical)
        if (fallback && logical != null) load(slot, view, logical)
        slot.restoreScroll(view, fallback)
        return view
    }

    private fun load(slot: BrowserPageSlot, view: WebView, address: String) {
        val referer = slot.referer.takeIf { slot.sendReferer }
        slot.sendReferer = false
        if (referer == null) view.loadUrl(host.engineUrl(address))
        else view.loadUrl(host.engineUrl(address), mapOf("Referer" to referer))
    }

    private fun pauseCurrent() {
        current?.lastInactiveUptime = SystemClock.elapsedRealtime()
        liveView?.onPause()
        sweep(3, 2)
    }

    private fun discardForward() {
        while (slots.lastIndex > index) releaseView(slots.removeAt(slots.lastIndex), save = false)
    }

    private fun sweep(before: Int, after: Int) {
        val now = SystemClock.elapsedRealtime()
        slots.forEachIndexed { position, slot ->
            if (position != index && slot.view != null &&
                (position < index - before || position > index + after || now - slot.lastInactiveUptime > 300_000))
                releaseView(slot, save = true)
        }
    }

    private fun releaseView(slot: BrowserPageSlot, save: Boolean) {
        val view = slot.view ?: return
        if (save) slot.capture()
        slot.cancelCallbacks()
        views.remove(view)
        try { PageViewLifecycle.destroy(view) } finally { slot.view = null; slot.mediaPaused = false }
    }

    private fun publish(slot: BrowserPageSlot) {
        val view = slot.view
        host.metadata(slot.logicalUrl ?: view?.url, slot.title ?: view?.title, view?.progress ?: 100)
    }
}
