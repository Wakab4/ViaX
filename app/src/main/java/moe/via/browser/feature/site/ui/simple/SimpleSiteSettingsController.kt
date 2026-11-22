package moe.via.browser.feature.site.ui.simple

import android.app.Activity
import android.widget.ImageView
import moe.via.browser.R
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.site.SiteDecision
import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.site.data.SiteSettingsRepository
import moe.via.browser.feature.site.ui.common.NativeSitePage
import moe.via.browser.feature.site.ui.common.NativeSiteRow
import moe.via.browser.feature.useragent.data.UserAgentRepository

internal class SimpleSiteSettingsController(activity: Activity, repository: SiteSettingsRepository, settings: BrowserSettings,
    agents: UserAgentRepository, feature: SiteFeature, back: ImageView?) : NativeSitePage(activity, repository, settings, agents, feature, back) {
    override fun render() {
        if (closed) return
        val checked = global() == SiteDecision.ALLOW
        val detail = activity.getString(if (feature == SiteFeature.POPUPS) {
            if (checked) R.string.ref_site_allowed else R.string.ref_site_popup_ask_detail
        } else if (checked) R.string.ref_site_vibration_allowed_detail else R.string.ref_site_disabled)
        submit(listOf(NativeSiteRow.Toggle(1, title, checked, detail = detail)))
    }
    override fun clicked(row: NativeSiteRow) {
        if (closed || row !is NativeSiteRow.Toggle) return
        repository.setGlobal(feature, if (!row.checked) SiteDecision.ALLOW else if (feature == SiteFeature.POPUPS) SiteDecision.ASK else SiteDecision.DENY)
        render()
    }
}
