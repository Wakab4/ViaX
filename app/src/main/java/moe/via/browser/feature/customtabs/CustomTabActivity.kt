package moe.via.browser.feature.customtabs

import moe.via.browser.BrowserActivity

/** Shares every browser feature owner, while the host selects an ephemeral session and compact chrome. */
class CustomTabActivity : BrowserActivity() {
    override val customTabMode: Boolean get() = true
    override val browserNightOverride: Boolean? get() = CustomTabRequest.nightOverride(intent)
}
