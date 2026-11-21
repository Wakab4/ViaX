package moe.pure.browser.feature.customtabs

import moe.pure.browser.BrowserActivity

/** Shares every browser feature owner, while the host selects an ephemeral session and compact chrome. */
class CustomTabActivity : BrowserActivity() {
    override val customTabMode: Boolean get() = true
    override val browserNightOverride: Boolean? get() = CustomTabRequest.nightOverride(intent)
}
