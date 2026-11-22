package moe.via.browser.feature.site.ui.pages

import android.widget.ImageView
import moe.via.browser.R
import moe.via.browser.feature.site.ui.SiteSettingsNavigation
import moe.via.browser.feature.site.ui.all.AllSitesController
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.theme.ThemedActivity

class AllSiteSettingsFragment : SiteRecordPage() {
    override fun createContent(activity: ThemedActivity, back: ImageView) = AllSitesController(activity, repository) { domain ->
        SiteSettingsNavigation.edit(this, domain) { changed -> if (changed and 1 != 0) content?.render() }
    }
    override fun configure(page: PageScaffold) {
        page.addIconAction(R.drawable.ref_ic_add, getString(R.string.ref_site_all_add)) { (content as AllSitesController).add() }
    }
}
