package moe.via.browser.feature.site.ui.common

import android.app.Activity
import android.widget.ImageView
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.site.data.SiteSettingsRepository
import moe.via.browser.feature.site.ui.binary.BinarySiteDescriptor
import moe.via.browser.feature.site.ui.binary.BinarySiteSettingsController
import moe.via.browser.feature.site.ui.font.FontSiteSettingsController
import moe.via.browser.feature.site.ui.permission.PermissionSiteDescriptor
import moe.via.browser.feature.site.ui.permission.PermissionSiteSettingsController
import moe.via.browser.feature.site.ui.simple.SimpleSiteSettingsController
import moe.via.browser.feature.site.ui.useragent.UserAgentSiteSettingsController
import moe.via.browser.feature.useragent.data.UserAgentRepository
import moe.via.browser.feature.site.ui.cookies.CookiesSettingsController
import moe.via.browser.feature.site.ui.image.ImageSettingsController
import moe.via.browser.feature.site.ui.redirect.RedirectSettingsController

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
