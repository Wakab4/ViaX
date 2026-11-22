package moe.via.browser.feature.site.ui.pages

import android.widget.ImageView
import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.site.ui.common.NativeSiteSettingsFactory
import moe.via.browser.ui.theme.ThemedActivity

abstract class SiteFeatureFragment : SiteRecordPage() {
    protected abstract val feature: SiteFeature
    override fun createContent(activity: ThemedActivity, back: ImageView) =
        requireNotNull(NativeSiteSettingsFactory.create(feature, activity, repository, settings, agents, back))
}
