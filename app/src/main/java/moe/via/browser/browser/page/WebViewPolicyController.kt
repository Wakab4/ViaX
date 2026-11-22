package moe.via.browser.browser.page

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.home.HomeDocument
import moe.via.browser.feature.site.SiteDecision
import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.site.data.SiteSettingsRepository
import moe.via.browser.feature.useragent.data.UserAgentRepository
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.webview.compat.WebDarkMode
import moe.via.browser.webview.compat.NativeUserAgentMetadata
import moe.via.browser.core.network.BrowserNetworkState

/** Translates global and per-site preferences into engine settings. */
class WebViewPolicyController(private val context: Context, private val settings: BrowserSettings,
    private val sites: SiteSettingsRepository, private val agents: UserAgentRepository) {
    private fun value(url: String, feature: SiteFeature) = sites.resolve(url, feature, settings)
    fun isPrivate(url: String): Boolean = sites.resolve(url, SiteFeature.INCOGNITO, settings) == SiteDecision.ALLOW
    fun reloadFingerprint(): String {
        val prefs = settings.preferences
        val values = settings.values
        return listOf(prefs.javascript, prefs.userAgentId, prefs.desktopUserAgentId, prefs.desktop, prefs.imageMode,
            prefs.simplifyUserAgent, values.getLong("site.reload_revision", 0), values.getLong("ua.revision", 0),
            prefs.doNotTrack, prefs.saveData, prefs.globalPrivacyControl, prefs.disableWebRtc, prefs.forceZoom,
            prefs.vibrationAllowed, prefs.adblock, prefs.cookies, prefs.thirdPartyCookies, prefs.popupAllowed,
            prefs.locationEnabled, prefs.cameraAsk, prefs.microphoneAsk, prefs.backCache, prefs.safeBrowsingDisabled,
            prefs.scriptsEnabled).joinToString("|")
    }

    @Suppress("DEPRECATION")
    fun apply(tab: BrowserTab, view: WebView, backgroundColor: Int, pageUrl: String = tab.url) {
        val home = HomeDocument.isHome(context, pageUrl)
        val filePage = pageUrl.startsWith("file://", ignoreCase = true)
        val prefs = settings.preferences
        val internal = home || filePage || moe.via.browser.feature.network.NetworkLogPages.isPage(context, pageUrl)
        val activeSite = sites.forUrl(pageUrl)?.takeIf { it.enabled }
        val desktop = if (filePage) prefs.desktop else value(pageUrl, SiteFeature.DESKTOP) == SiteDecision.ALLOW
        val imageMode = value(pageUrl, SiteFeature.IMAGES)
        val wifi = BrowserNetworkState.type(context) != 2
        val images = home || filePage ||
            imageMode == SiteDecision.ALLOW || imageMode == SiteDecision.WIFI && wifi
        if (tab.liveView === view) tab.desktop = desktop
        view.setBackgroundColor(if (home || filePage) android.graphics.Color.TRANSPARENT else backgroundColor)
        view.settings.apply {
            javaScriptEnabled = internal || value(pageUrl, SiteFeature.JAVASCRIPT) == SiteDecision.ALLOW
            textZoom = if (home || filePage) 100 else value(pageUrl, SiteFeature.FONT_SIZE).coerceAtLeast(1)
            loadsImagesAutomatically = images
            blockNetworkImage = !images
            // Native routes every popup through onCreateWindow, where the site policy is decided.
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(prefs.supportMultipleWindows)
            // Native leaves the engine enabled and decides geolocation in its permission callback.
            setGeolocationEnabled(true)
            userAgentString = if (home || filePage) agents.resolve(settings, prefs.desktop)
                else agents.resolve(settings, desktop,
                    activeSite?.let { it.native?.userAgentChoice ?: it.values[SiteFeature.USER_AGENT] ?: -1000 }, activeSite?.customUserAgent)
            NativeUserAgentMetadata.apply(this)
            safeBrowsingEnabled = !prefs.safeBrowsingDisabled
            allowFileAccess = true
            allowContentAccess = true
        }
        WebDarkMode.apply(view, settings.night, prefs.forceDark)
        CookieManager.getInstance().apply {
            val cookies = sites.global(SiteFeature.COOKIES, settings) == SiteDecision.ALLOW
            setAcceptCookie(cookies)
            setAcceptThirdPartyCookies(view, cookies && sites.global(SiteFeature.THIRD_PARTY_COOKIES, settings) == SiteDecision.ALLOW)
        }
        WebView.setWebContentsDebuggingEnabled(prefs.webDebugging)
        ProfileHeaders.apply(prefs)
    }
}
