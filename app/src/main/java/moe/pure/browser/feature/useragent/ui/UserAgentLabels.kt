package moe.pure.browser.feature.useragent.ui

import android.content.Context
import moe.pure.browser.R
import moe.pure.browser.feature.useragent.UserAgent

/** z8.b4.b preset IDs keep their original localized labels; saved names stay untouched. */
object UserAgentLabels {
    fun label(context: Context, agent: UserAgent): String {
        val resource = when (agent.id) {
        0 -> R.string.ref_site_ua_default
        -1 -> R.string.ref_site_ua_android_mobile
        -2 -> R.string.ref_site_ua_android_tablet
        -3 -> R.string.ref_site_ua_windows_chrome
        -4 -> R.string.ref_site_ua_windows_ie
        -5 -> R.string.ref_site_ua_mac_chrome
        -6 -> R.string.ref_site_ua_ios
        -7 -> R.string.ref_site_ua_mac_safari
        -8 -> R.string.ref_site_ua_symbian
            else -> return agent.name
        }
        return context.getString(resource)
    }
}
