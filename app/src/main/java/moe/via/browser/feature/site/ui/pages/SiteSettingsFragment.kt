package moe.via.browser.feature.site.ui.pages

import android.view.View
import android.widget.ImageView
import moe.via.browser.feature.site.ui.SiteSettingsNavigation
import moe.via.browser.feature.site.ui.index.SiteSettingsIndex
import moe.via.browser.ui.pages.RecordPageScope
import moe.via.browser.ui.theme.ThemedActivity

class SiteSettingsFragment : SiteRecordPage() {
    override fun createContent(activity: ThemedActivity, back: ImageView) = SiteSettingsIndex(activity, repository, settings, agents,
        { RecordPageScope.of(this)?.push(AllSiteSettingsFragment::class.java, null) },
        { SiteSettingsNavigation.feature(this, it) })
    override fun onHiddenChanged(hidden: Boolean) { super.onHiddenChanged(hidden); if (!hidden && view != null) content?.render() }
}
