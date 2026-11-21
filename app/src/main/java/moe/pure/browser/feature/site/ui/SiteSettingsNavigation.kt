package moe.pure.browser.feature.site.ui

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import moe.pure.browser.browser.host.BrowserRootFragment
import moe.pure.browser.feature.site.NativeSiteKeys
import moe.pure.browser.feature.site.SiteFeature
import moe.pure.browser.feature.site.ui.pages.SiteSettingsFragment
import moe.pure.browser.feature.site.ui.pages.SingleSiteSettingsFragment
import moe.pure.browser.ui.pages.RecordPageFragment
import moe.pure.browser.ui.pages.RecordPageScope
import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.feature.site.ui.font.FontSiteSettingsFragment
import moe.pure.browser.feature.site.ui.useragent.UserAgentSiteSettingsFragment
import moe.pure.browser.feature.site.ui.binary.DesktopSiteSettingsFragment
import moe.pure.browser.feature.site.ui.image.ImageSiteSettingsFragment
import moe.pure.browser.feature.site.ui.binary.JavascriptSiteSettingsFragment
import moe.pure.browser.feature.site.ui.cookies.CookiesSiteSettingsFragment
import moe.pure.browser.feature.site.ui.simple.PopupsSiteSettingsFragment
import moe.pure.browser.feature.site.ui.binary.AdblockSiteSettingsFragment
import moe.pure.browser.feature.site.ui.permission.ClipboardSiteSettingsFragment
import moe.pure.browser.feature.site.ui.permission.OpenAppsSiteSettingsFragment
import moe.pure.browser.feature.site.ui.permission.LocationSiteSettingsFragment
import moe.pure.browser.feature.site.ui.permission.CameraSiteSettingsFragment
import moe.pure.browser.feature.site.ui.permission.MicrophoneSiteSettingsFragment
import moe.pure.browser.feature.site.ui.redirect.RedirectSiteSettingsFragment
import moe.pure.browser.feature.site.ui.simple.VibrationSiteSettingsFragment
import moe.pure.browser.feature.site.ui.binary.BackCacheSiteSettingsFragment

/** Actual callers own the native result listener; no Activity request code or domain result bridge. */
object SiteSettingsNavigation {
    fun open(activity: Activity, url: String? = null, flags: Int = 2) {
        val owner = activity as ThemedActivity
        if (url.isNullOrEmpty() || url.startsWith("file://", true)) owner.pageStack.push(SiteSettingsFragment::class.java, null)
        else {
            val domain = NativeSiteKeys.fromUrl(url)
            if (domain.isEmpty()) return
            val root = requireNotNull(BrowserRootFragment.find(owner))
            edit(root, domain, flags, root::siteSettingsChanged)
        }
    }
    fun feature(activity: Activity, feature: SiteFeature) {
        (activity as ThemedActivity).pageStack.push(page(feature), null)
    }
    internal fun feature(owner: Fragment, feature: SiteFeature) {
        if (owner.isVisible && !owner.isRemoving) RecordPageScope.of(owner)?.push(page(feature), null)
    }
    internal fun edit(owner: Fragment, domain: String, flags: Int = 0, result: (Int) -> Unit) {
        val manager = owner.parentFragmentManager
        manager.setFragmentResultListener(SingleSiteSettingsFragment.RESULT, owner) { _, bundle ->
            result(bundle.getInt("changed", 0)); manager.clearFragmentResultListener(SingleSiteSettingsFragment.RESULT)
        }
        if (owner.isVisible && !owner.isRemoving) RecordPageScope.of(owner)?.push(SingleSiteSettingsFragment::class.java,
            Bundle().apply { putString("domain", domain); putInt("flags", flags) })
    }
    private fun page(feature: SiteFeature): Class<out RecordPageFragment> = when (feature) {
        SiteFeature.FONT_SIZE -> FontSiteSettingsFragment::class.java
        SiteFeature.USER_AGENT -> UserAgentSiteSettingsFragment::class.java
        SiteFeature.DESKTOP -> DesktopSiteSettingsFragment::class.java
        SiteFeature.IMAGES -> ImageSiteSettingsFragment::class.java
        SiteFeature.JAVASCRIPT -> JavascriptSiteSettingsFragment::class.java
        SiteFeature.COOKIES -> CookiesSiteSettingsFragment::class.java
        SiteFeature.POPUPS -> PopupsSiteSettingsFragment::class.java
        SiteFeature.ADBLOCK -> AdblockSiteSettingsFragment::class.java
        SiteFeature.CLIPBOARD -> ClipboardSiteSettingsFragment::class.java
        SiteFeature.OPEN_APPS -> OpenAppsSiteSettingsFragment::class.java
        SiteFeature.LOCATION -> LocationSiteSettingsFragment::class.java
        SiteFeature.CAMERA -> CameraSiteSettingsFragment::class.java
        SiteFeature.MICROPHONE -> MicrophoneSiteSettingsFragment::class.java
        SiteFeature.REDIRECTS -> RedirectSiteSettingsFragment::class.java
        SiteFeature.VIBRATION -> VibrationSiteSettingsFragment::class.java
        SiteFeature.BACK_CACHE -> BackCacheSiteSettingsFragment::class.java
        else -> error("No standalone page for $feature")
    }
}

