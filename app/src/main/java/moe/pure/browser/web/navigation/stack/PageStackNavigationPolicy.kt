package moe.pure.browser.web.navigation.stack

import android.content.Context
import android.webkit.URLUtil
import moe.pure.browser.core.BrowserInternalPages

/** Native ba.b values supplied by the existing application-owned settings repository. */
data class PageStackSitePolicy(
    val enabled: Boolean,
    val backCache: Boolean? = null,
    val userAgentChoice: Int = -1000,
    val customUserAgent: String? = null,
    val desktopOverride: Boolean? = null,
    val hasSettings: Boolean = true
)

internal data class PageStackDecision(val code: Int, val settingsUrl: String?,
    val prepare: Boolean = true, val delayedReload: Boolean = false)

/** e8.i.K/N/E: this controls independent WebViews, not Chromium's back-forward cache. */
class PageStackNavigationPolicy(context: Context,
    private val globalBackCache: () -> Boolean = { false },
    private val site: (String) -> PageStackSitePolicy? = { null },
    private val popupSite: (String) -> PageStackSitePolicy? = site) {
    private val app = context.applicationContext

    internal fun decide(source: String?, target: String, automatic: Boolean, popupFirst: Boolean): PageStackDecision {
        if (popupFirst && URLUtil.isNetworkUrl(target)) {
            val targetSite = popupSite(target)
            if (targetSite == null || !targetSite.hasSettings) return PageStackDecision(2, target, prepare = false)
            val reload = targetSite.userAgentChoice != -1000 || !targetSite.customUserAgent.isNullOrEmpty() ||
                targetSite.desktopOverride != null
            return PageStackDecision(if (reload) 3 else 2, target, delayedReload = reload)
        }
        if (needsNewPage(source, target, automatic)) return PageStackDecision(1, target)
        var settingsUrl = target
        if (automatic && source != null) {
            val previous = site(source)
            val next = site(target)
            if (previous?.enabled == true && next?.enabled != true &&
                (previous.userAgentChoice != -1 || previous.desktopOverride == true)) settingsUrl = source
        }
        return PageStackDecision(2, settingsUrl)
    }

    private fun needsNewPage(source: String?, target: String, automatic: Boolean): Boolean {
        val previousUrl = source.orEmpty()
        val generated = BrowserInternalPages.isInternal(app, target)
        if (generated != BrowserInternalPages.isInternal(app, previousUrl)) return true
        if (generated) return false
        if (automatic) return false
        val targetHash = target.lastIndexOf('#')
        val sourceHash = previousUrl.lastIndexOf('#')
        if (targetHash >= 0 || sourceHash >= 0) {
            val targetEnd = if (targetHash < 0) target.length else targetHash
            val sourceEnd = if (sourceHash < 0) previousUrl.length else sourceHash
            if (targetEnd == sourceEnd && target.regionMatches(0, previousUrl, 0, targetEnd)) return false
        }
        return (backCache(target) || backCache(previousUrl)) && !isAuthenticationUrl(previousUrl)
    }

    private fun backCache(url: String): Boolean {
        val setting = site(url)
        if (setting?.enabled == true && setting.backCache != null) return setting.backCache
        return globalBackCache() && defaultBackCacheDomain(authority(url))
    }

    private fun authority(url: String): String {
        val separator = url.indexOf("://")
        if (separator < 0) return ""
        val start = separator + 3
        val end = url.indexOf('/', start).takeIf { it >= 0 } ?: url.length
        return url.substring(start, end)
    }

    /** GP z8.b0.J intentionally uses case-sensitive equality/contains against this authority. */
    private fun defaultBackCacheDomain(domain: String): Boolean =
        !(domain.startsWith("192.168.") || domain == "x.com" || domain == "metaso.cn" ||
            listOf("forum.softpedia.com", "3g.163.com", "bbs.binmt.cc", "www.giant.com.cn", "www.10099.com.cn",
                ".10086.cn", ".10010.com", "myaccount.google.com", "accounts.google.com").any(domain::contains))

    private fun isAuthenticationUrl(url: String): Boolean {
        if (url.length < 5) return false
        for (word in AUTH_WORDS) {
            val index = url.indexOf(word, ignoreCase = true)
            if (index <= 0) continue
            val before = url[index - 1]
            if (asciiLetter(before) || before == '=') return false
            val end = index + word.length
            if (end >= url.length) return true
            val after = url[end]
            return !(before == '.' && after == '.') && !asciiLetter(after)
        }
        return false
    }

    private fun asciiLetter(value: Char) = value in 'a'..'z' || value in 'A'..'Z'
    companion object { private val AUTH_WORDS = arrayOf("login", "signup", "signin", "register", "sign-up", "create-account", "createaccount", "auth") }
}
