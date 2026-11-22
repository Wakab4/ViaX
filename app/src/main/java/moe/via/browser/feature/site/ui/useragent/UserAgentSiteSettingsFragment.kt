package moe.via.browser.feature.site.ui.useragent

import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.site.ui.pages.SiteFeatureFragment

class UserAgentSiteSettingsFragment : SiteFeatureFragment() {
    override val feature = SiteFeature.USER_AGENT
    override fun onHiddenChanged(hidden: Boolean) { super.onHiddenChanged(hidden); if (!hidden && view != null) content?.render() }
}

