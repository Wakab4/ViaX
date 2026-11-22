package moe.via.browser.browser.navigation.redirect

import android.app.Activity
import android.os.SystemClock
import android.webkit.WebView
import moe.via.browser.R
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.site.SiteDecision
import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.site.data.SiteSettingsRepository
import moe.via.browser.browser.session.BrowserTab

/** s6.p.E is the first Native navigation consumer, before popup/external-app handling. */
class PageRedirectController(private val activity: Activity, private val settings: BrowserSettings,
    private val sites: SiteSettingsRepository, private val host: Host) {
    interface Host {
        fun currentTab(): BrowserTab?
        fun pageVisible(): Boolean
        fun elementPickerActive(): Boolean
        fun navigate(tab: BrowserTab, url: String)
    }
    private val notice = RedirectNotice(activity)
    private var pending: RedirectNoticeState? = null
    private var disposed = false

    /** Native deliberately ignores isRedirect/hasGesture and does not filter isForMainFrame here. */
    fun shouldOverride(sourceView: WebView, targetUrl: String, silent: Boolean = false): Boolean {
        if (disposed) return false
        if (host.elementPickerActive()) return true
        val sourceUrl = sourceView.url.orEmpty()
        if (!RedirectDomains.network(sourceUrl) || !RedirectDomains.network(targetUrl)) return false
        // The Native setting has allow/ask, with an inherited per-site Boolean override.
        if (sites.resolve(sourceUrl, SiteFeature.REDIRECTS, settings) == SiteDecision.ALLOW) return false
        val source = RedirectDomains.base(sourceUrl)
        val target = RedirectDomains.base(targetUrl)
        if (source.isNotEmpty() && target.equals(source, true)) return false
        if (!silent && host.pageVisible() && !activity.isDestroyed && !activity.isFinishing) {
            val state = RedirectNoticeState(host.currentTab()?.id, targetUrl, target, SystemClock.uptimeMillis() + 3500L)
            val previous = pending
            pending = state
            if (!show(state) && pending === state) pending = previous
        }
        return true
    }

    /** Chrome replacement preserves the original message deadline instead of restarting its timeout. */
    fun rebind() {
        if (disposed) return
        val state = pending
        pending = null
        notice.close(false)
        state?.let {
            if (it.expiresAt > SystemClock.uptimeMillis() && host.pageVisible()) {
                pending = it; show(it)
            }
        }
    }

    private fun show(state: RedirectNoticeState): Boolean {
        val remaining = state.expiresAt - SystemClock.uptimeMillis()
        if (remaining <= 0) { pending = null; notice.close(false); return false }
        val text = if (state.targetDomain.isEmpty()) activity.getString(R.string.ref_redirect_question)
            else activity.getString(R.string.ref_redirect_target_question, state.targetDomain)
        val shown = notice.show(text, activity.getString(R.string.ref_redirect_allow), clicked = action@{
            if (disposed) return@action
            if (pending === state) pending = null
            val current = host.currentTab()
            // s6.p.G checks only current tab ID; same-tab navigation and WebView replacement do not revoke it.
            if (current != null && current.id == state.selectedTabId) host.navigate(current, state.targetUrl)
        }, timeoutMillis = remaining, onRemoved = { if (pending === state) pending = null })
        return shown
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        pending = null; notice.close(false)
    }
}
