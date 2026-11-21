package moe.pure.browser.model

import android.content.Context
import android.webkit.WebView
import moe.pure.browser.browser.TabSnapshot
import moe.pure.browser.browser.state.TabNativeStateCodec
import moe.pure.browser.feature.home.HomeDocument
import moe.pure.browser.feature.network.PageRequestLog
import moe.pure.browser.web.navigation.stack.BrowserPageStack
import moe.pure.browser.web.navigation.stack.PageStackNavigationPolicy
import moe.pure.browser.web.selection.BrowserPageView

/** One runtime tab owns a Native page stack; each page has a separate WebView and engine history. */
class BrowserTab(private val context: Context, snapshot: TabSnapshot,
    private val configure: (BrowserTab, WebView) -> Unit,
    navigationPolicy: PageStackNavigationPolicy = PageStackNavigationPolicy(context),
    private val pageHost: PageHost = object : PageHost {}) {
    interface PageHost {
        fun prepare(tab: BrowserTab, view: WebView, url: String?) = Unit
        /** Native D flags: 1 selected runtime tab, 2 outer back, 4 outer forward/new, 9 activation. */
        fun changed(tab: BrowserTab, previous: WebView?, current: WebView?, flags: Int) = Unit
    }
    private var saved = restoredSnapshot(context, snapshot)
    var home = saved.home
    var desktop = saved.desktop
    var title = saved.title
    var url = saved.url
    val id get() = saved.id
    val persistence get() = saved.persistence
    var lastVisitedAt = saved.lastVisitedAt
    var progress = 100
    var hasPrivateHistory = false
    val requests = PageRequestLog()
    var runtime = moe.pure.browser.browser.TabRuntimeState()
        internal set
    private var disposed = false
    private val pages = BrowserPageStack(saved.url, saved.state, navigationPolicy, object : BrowserPageStack.Host {
        override fun create(pageId: Int): BrowserPageView {
            val view = try { BrowserPageView(context, pageId) }
            catch (error: RuntimeException) { throw moe.pure.browser.web.compat.WebEngineStartupException(error) }
            catch (error: LinkageError) { throw moe.pure.browser.web.compat.WebEngineStartupException(error) }
            configure(this@BrowserTab, view)
            return view
        }
        override fun prepare(view: WebView, url: String?) = pageHost.prepare(this@BrowserTab, view, url)
        override fun changed(previous: WebView?, current: WebView?, flags: Int) {
            current?.let { (it as? BrowserPageView)?.pageId?.let(requests::selectPage) }
            pageHost.changed(this@BrowserTab, previous, current, flags)
        }
        override fun metadata(url: String?, title: String?, progress: Int) {
            if (url != null) this@BrowserTab.url = normalizeAddress(url)
            home = if (this@BrowserTab.url == saved.literalStartUrl) moe.pure.browser.core.BrowserInternalPages.kind(context, this@BrowserTab.url) == 1
                else HomeDocument.isHome(context, this@BrowserTab.url)
            this@BrowserTab.title = title.orEmpty()
            this@BrowserTab.progress = progress
        }
        override fun engineUrl(url: String) = normalizeAddress(url)
        override fun logicalUrl(url: String, previousUrl: String?): String {
            val logical = moe.pure.browser.web.error.BrowserErrorController.logicalUrl(normalizeAddress(url))
            return if (previousUrl?.startsWith("view-source:") == true && !logical.startsWith("view-source:"))
                "view-source:$logical" else logical
        }
    })

    val liveView: WebView? get() = pages.liveView
    val webView: WebView get() = pages.requireCurrent()
    fun owns(view: WebView): Boolean = pages.owns(view)
    fun isCurrent(view: WebView): Boolean = pages.isCurrent(view)
    fun pageId(view: WebView): Int? = pages.pageId(view)
    fun sourceIdentity(view: WebView): String? = pages.sourceIdentity(view)
    fun liveSource(identity: String): WebView? = pages.liveSource(identity)
    fun sourceUrl(view: WebView): String? = pages.sourceUrl(view)
    fun beforeNavigation(source: WebView, url: String): Boolean? = pages.beforeNavigation(source, url)
    fun shouldOverride(source: WebView, url: String, redirectOrNoGesture: Boolean): Boolean =
        pages.shouldOverride(source, url, redirectOrNoGesture)
    fun pageStarted(source: WebView, url: String) = pages.pageStarted(source, url)
    fun pageFinished(source: WebView, url: String) = pages.pageFinished(source, url)
    fun urlChanged(source: WebView, url: String) = pages.pageStarted(source, url)
    fun titleChanged(source: WebView, title: String?) = pages.titleChanged(source, title)
    fun progressChanged(source: WebView, progress: Int) = pages.progressChanged(source, progress)
    fun preparePopup(sourceUrl: String?) = pages.preparePopup(sourceUrl)
    fun canGoBack(): Boolean = pages.canGoBack()
    fun canGoForward(): Boolean = pages.canGoForward()
    fun goBack() = pages.goBack()
    fun goForward() = pages.goForward()
    fun reload() = pages.reload()
    fun stop() = pages.stop()
    fun activate() = pages.activate()
    fun deactivate() { lastVisitedAt = System.currentTimeMillis(); pages.deactivate() }
    fun pause() = pages.pause()
    fun resume() = pages.resume()
    fun trimMemory(keep: Int) = pages.trimMemory(keep, title, url)
    fun reusePage(matcher: (String) -> Boolean): Boolean {
        val urls = pages.urls()
        for (index in urls.indices.reversed()) {
            val address = urls[index] ?: continue
            if (matcher(address) && pages.reusePage(index)) return true
        }
        return false
    }

    fun navigate(address: String) {
        pages.navigate(normalizeAddress(address))
    }

    fun normalizeAddress(address: String): String = if (address == saved.literalStartUrl) address else HomeDocument.normalize(context, address)

    fun navigateConfigured(address: String) {
        saved = saved.copy(literalStartUrl = address)
        navigate(address)
    }

    fun snapshot(): TabSnapshot {
        if (disposed) return saved
        val view = liveView
        val state = pages.snapshot(title, url)
        saved = saved.copy(url = url, title = title, home = home, desktop = desktop,
            scrollX = view?.scrollX ?: saved.scrollX, scrollY = view?.scrollY ?: saved.scrollY,
            state = state, lastVisitedAt = lastVisitedAt)
        return saved
    }

    fun release() {
        if (disposed) return
        snapshot()
        pages.release(title, url)
    }

    fun dispose() { if (!disposed) { disposed = true; pages.dispose() } }

    companion object {
        private fun restoredSnapshot(context: Context, value: TabSnapshot): TabSnapshot {
            val outer = TabNativeStateCodec.decode(value.state)
            val storedUrl = outer?.getString("URL") ?: value.url
            val address = if (storedUrl == value.literalStartUrl) storedUrl else HomeDocument.normalize(context, storedUrl)
            return value.copy(url = address, title = outer?.getString("TITLE") ?: value.title,
                home = if (address == value.literalStartUrl) moe.pure.browser.core.BrowserInternalPages.kind(context, address) == 1
                    else HomeDocument.isHome(context, address))
        }
    }
}
