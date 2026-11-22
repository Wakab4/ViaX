package moe.via.browser.feature.site.ui.binary

import moe.via.browser.R
import moe.via.browser.feature.site.SiteFeature

internal data class BinarySiteDescriptor(val feature: SiteFeature, val addEnable: Int, val addDisable: Int,
    val onOff: Boolean, val enabledDetail: Int? = null) {
    companion object {
        fun forFeature(feature: SiteFeature): BinarySiteDescriptor? = when (feature) {
            SiteFeature.DESKTOP -> BinarySiteDescriptor(feature, R.string.ref_site_desktop_add_enable, R.string.ref_site_desktop_add_disable, true)
            SiteFeature.JAVASCRIPT -> BinarySiteDescriptor(feature, R.string.ref_site_javascript_add_allow, R.string.ref_site_javascript_add_block, false, R.string.ref_site_javascript_enabled_detail)
            SiteFeature.ADBLOCK -> BinarySiteDescriptor(feature, R.string.ref_site_adblock_add_enable, R.string.ref_site_adblock_add_disable, true)
            SiteFeature.BACK_CACHE -> BinarySiteDescriptor(feature, R.string.ref_site_back_cache_add_enable, R.string.ref_site_back_cache_add_disable, true)
            else -> null
        }
    }
}
