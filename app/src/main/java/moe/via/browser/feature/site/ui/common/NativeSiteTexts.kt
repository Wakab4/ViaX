package moe.via.browser.feature.site.ui.common

import android.app.Activity
import moe.via.browser.R
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.site.SiteDecision
import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.useragent.data.UserAgentRepository
import moe.via.browser.feature.useragent.data.UserAgent
import android.content.Context
import java.util.Locale

/** Shared labels only; callers retain the Native distinction between inherited and explicit values. */
class NativeSiteTexts(private val activity: Activity, private val settings: BrowserSettings,
    private val agents: UserAgentRepository) {
    val on get() = activity.getString(R.string.ref_site_on)
    val off get() = activity.getString(R.string.ref_site_off)
    val allowed get() = activity.getString(R.string.ref_site_allowed)
    val blocked get() = activity.getString(R.string.ref_site_blocked)
    val allow get() = activity.getString(R.string.ref_site_allow)
    val block get() = activity.getString(R.string.ref_site_block)
    val ask get() = activity.getString(R.string.ref_site_ask)
    val add get() = activity.getString(R.string.ref_site_add)
    val delete get() = activity.getString(R.string.ref_site_delete)
    val custom get() = activity.getString(R.string.ref_site_custom)
    fun defaultLabel(text: String) = activity.getString(R.string.ref_site_default_format, text)

    fun title(feature: SiteFeature): String = activity.getString(when (feature) {
        SiteFeature.FONT_SIZE -> R.string.ref_site_font_title
        SiteFeature.USER_AGENT -> R.string.ref_site_user_agent_title
        SiteFeature.DESKTOP -> R.string.ref_site_desktop_title
        SiteFeature.JAVASCRIPT -> R.string.ref_site_javascript_title
        SiteFeature.ADBLOCK -> R.string.ref_site_adblock_title
        SiteFeature.BACK_CACHE -> R.string.ref_site_back_cache_title
        SiteFeature.CLIPBOARD -> R.string.ref_site_clipboard_title
        SiteFeature.OPEN_APPS -> R.string.ref_site_open_apps_title
        SiteFeature.LOCATION -> R.string.ref_site_location_title
        SiteFeature.CAMERA -> R.string.ref_site_camera_title
        SiteFeature.MICROPHONE -> R.string.ref_site_microphone_title
        SiteFeature.POPUPS -> R.string.ref_site_popup_title
        SiteFeature.VIBRATION -> R.string.ref_site_vibration_title
        SiteFeature.IMAGES -> R.string.ref_image_page_title
        SiteFeature.COOKIES -> R.string.ref_cookie_page_title
        SiteFeature.THIRD_PARTY_COOKIES -> R.string.ref_cookie_page_block_third_party
        SiteFeature.REDIRECTS -> R.string.ref_redirect_page_title
        SiteFeature.INCOGNITO -> R.string.ref_site_incognito_title
    })

    fun summary(feature: SiteFeature, value: Int, customUa: String? = null): String = when (feature) {
        SiteFeature.FONT_SIZE -> String.format(Locale.ROOT, "%d%%", value)
        SiteFeature.USER_AGENT -> if (value == -999) customUa.orEmpty() else agents.all().firstOrNull { it.id == value }?.let { userAgentLabel(activity, it) }.orEmpty()
        SiteFeature.DESKTOP, SiteFeature.ADBLOCK, SiteFeature.INCOGNITO, SiteFeature.BACK_CACHE -> if (value == SiteDecision.ALLOW) on else off
        SiteFeature.IMAGES -> if (value == SiteDecision.WIFI) activity.getString(R.string.ref_site_image_wifi) else if (value == SiteDecision.ALLOW) allowed else blocked
        SiteFeature.REDIRECTS -> if (value == SiteDecision.ALLOW) allowed else activity.getString(R.string.ref_site_redirect_ask)
        else -> when (value) { SiteDecision.ALLOW -> allowed; SiteDecision.ASK -> ask; else -> blocked }
    }

    companion object {
        fun userAgentLabel(context: Context, value: UserAgent): String {
            val resource = when (value.id) {
                0 -> R.string.ref_site_ua_default
                -1 -> R.string.ref_site_ua_android_mobile
                -2 -> R.string.ref_site_ua_android_tablet
                -3 -> R.string.ref_site_ua_windows_chrome
                -4 -> R.string.ref_site_ua_windows_ie
                -5 -> R.string.ref_site_ua_mac_chrome
                -6 -> R.string.ref_site_ua_ios
                -7 -> R.string.ref_site_ua_mac_safari
                -8 -> R.string.ref_site_ua_symbian
                else -> return value.name
            }
            return context.getString(resource)
        }
    }
}
