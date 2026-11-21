package moe.pure.browser.feature.site.ui.common

import android.app.Activity
import android.widget.ImageView
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.site.SiteFeature
import moe.pure.browser.feature.site.SiteSettingsRepository
import moe.pure.browser.feature.site.ui.binary.BinarySiteDescriptor
import moe.pure.browser.feature.site.ui.binary.BinarySiteSettingsController
import moe.pure.browser.feature.site.ui.font.FontSiteSettingsController
import moe.pure.browser.feature.site.ui.permission.PermissionSiteDescriptor
import moe.pure.browser.feature.site.ui.permission.PermissionSiteSettingsController
import moe.pure.browser.feature.site.ui.simple.SimpleSiteSettingsController
import moe.pure.browser.feature.site.ui.useragent.UserAgentSiteSettingsController
import moe.pure.browser.feature.useragent.UserAgentRepository
import moe.pure.browser.feature.site.ui.cookies.CookiesSettingsController
import moe.pure.browser.feature.site.ui.image.ImageSettingsController
import moe.pure.browser.feature.site.ui.redirect.RedirectSettingsController

object NativeSiteSettingsFactory {
    fun create(feature: SiteFeature, activity: Activity, repository: SiteSettingsRepository, settings: BrowserSettings,
        agents: UserAgentRepository, backImage: ImageView? = null): NativeSiteSettingsOwner? {
        BinarySiteDescriptor.forFeature(feature)?.let { return BinarySiteSettingsController(activity, repository, settings, agents, it, backImage) }
        PermissionSiteDescriptor.forFeature(feature)?.let { return PermissionSiteSettingsController(activity, repository, settings, agents, it, backImage) }
        return when (feature) {
            SiteFeature.COOKIES -> CookiesSettingsController(activity, repository, settings, backImage)
            SiteFeature.IMAGES -> ImageSettingsController(activity, repository, settings, backImage)
            SiteFeature.REDIRECTS -> RedirectSettingsController(activity, repository, settings, backImage)
            SiteFeature.POPUPS, SiteFeature.VIBRATION -> SimpleSiteSettingsController(activity, repository, settings, agents, feature, backImage)
            SiteFeature.FONT_SIZE -> FontSiteSettingsController(activity, repository, settings, agents, backImage)
            SiteFeature.USER_AGENT -> UserAgentSiteSettingsController(activity, repository, settings, agents, backImage)
            else -> null
        }
    }
}
