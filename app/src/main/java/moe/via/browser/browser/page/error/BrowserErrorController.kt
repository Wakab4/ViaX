package moe.via.browser.browser.page.error

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import moe.via.browser.browser.session.BrowserTab
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import moe.via.browser.browser.navigation.links.BrowserErrorLinks

/** GP p4.j -> e8.i.u: route unknown schemes; retain the engine's ordinary error document. */
class BrowserErrorController(private val host: Host) {
    interface Host {
        /** Delegate to the existing internal/external URL router. Native ignores this return value. */
        fun routeUnknownScheme(tab: BrowserTab, view: WebView, url: String): Boolean
        /** Load the decoded destination in the originating tab, without interpreting it as a query. */
        fun navigate(tab: BrowserTab, view: WebView, url: String)
    }
    private class State(var generation: Long = 0, var failure: BrowserPageFailure? = null)
    private val handler = Handler(Looper.getMainLooper())
    private val states = WeakHashMap<WebView, State>()
    private var disposed = false

    /** Only main-frame failures belong here; subresource failures must not replace the document. */
    fun onError(tab: BrowserTab, view: WebView, code: Int, description: String?, failingUrl: String?): Boolean {
        if (disposed || !tab.owns(view) || failingUrl == null) return false
        val state = states.getOrPut(view, ::State)
        state.failure = BrowserPageFailure(code, description, failingUrl)
        if (code != WebViewClient.ERROR_UNSUPPORTED_SCHEME) return false
        if (!consumeJump(tab, view, failingUrl)) host.routeUnknownScheme(tab, view, failingUrl)
        return true
    }

    /** Consume before generic file/external handling; a wrapped javascript: target never executes. */
    fun consumeJump(tab: BrowserTab, view: WebView, url: String): Boolean {
        val target = BrowserErrorLinks.target(url) ?: return false
        if (disposed || !BrowserErrorLinks.canNavigate(target)) return true
        if (!tab.owns(view)) return true
        val state = states.getOrPut(view, ::State)
        val generation = state.generation
        val tabReference = WeakReference(tab)
        val viewReference = WeakReference(view)
        handler.post {
            val owner = tabReference.get()
            val originatingView = viewReference.get()
            if (!disposed && owner != null && originatingView != null && owner.owns(originatingView) &&
                states[originatingView]?.generation == generation
            ) host.navigate(owner, originatingView, target)
        }
        return true
    }

    /** Call on a real main-frame navigation; invalidates pending jumps and old failure metadata. */
    fun navigation(tab: BrowserTab, view: WebView) {
        if (!tab.owns(view)) return
        val state = states.getOrPut(view, ::State)
        state.generation++; state.failure = null
    }

    fun failure(tab: BrowserTab): BrowserPageFailure? = tab.liveView?.let { states[it]?.failure }
    fun released(view: WebView) { states.remove(view) }

    fun dispose() { disposed = true; handler.removeCallbacksAndMessages(null); states.clear() }

}
