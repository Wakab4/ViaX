package moe.via.browser.feature.site.ui.permission

import moe.via.browser.R
import moe.via.browser.feature.site.SiteFeature

internal data class PermissionSiteDescriptor(val feature: SiteFeature, val checkbox: Boolean,
    val addAllow: Int, val addBlock: Int, val askDetail: Int, val blockedDetail: Int,
    val allowedDetail: Int = 0) {
    companion object {
        fun forFeature(feature: SiteFeature): PermissionSiteDescriptor? = when (feature) {
            SiteFeature.CLIPBOARD -> PermissionSiteDescriptor(feature, false, R.string.ref_site_clipboard_add_allow,
                R.string.ref_site_clipboard_add_block, R.string.ref_site_clipboard_ask_detail,
                R.string.ref_site_clipboard_block_detail, R.string.ref_site_clipboard_allow_detail)
            SiteFeature.OPEN_APPS -> PermissionSiteDescriptor(feature, false, R.string.ref_site_open_apps_add_allow,
                R.string.ref_site_open_apps_add_block, R.string.ref_site_open_apps_ask_detail,
                R.string.ref_site_open_apps_block_detail, R.string.ref_site_open_apps_allow_detail)
            SiteFeature.LOCATION -> PermissionSiteDescriptor(feature, true, R.string.ref_site_location_add_allow,
                R.string.ref_site_location_add_block, R.string.ref_site_location_ask_detail, R.string.ref_site_blocked)
            SiteFeature.CAMERA -> PermissionSiteDescriptor(feature, true, R.string.ref_site_camera_add_allow,
                R.string.ref_site_camera_add_block, R.string.ref_site_camera_ask_detail, R.string.ref_site_disabled)
            SiteFeature.MICROPHONE -> PermissionSiteDescriptor(feature, true, R.string.ref_site_microphone_add_allow,
                R.string.ref_site_microphone_add_block, R.string.ref_site_microphone_ask_detail, R.string.ref_site_disabled)
            else -> null
        }
    }
}
