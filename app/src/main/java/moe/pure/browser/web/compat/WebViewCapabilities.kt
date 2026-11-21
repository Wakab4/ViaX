package moe.pure.browser.web.compat

import android.os.Build

object WebViewCapabilities {
    /** Native w9.r.h is the SDK29 provider strategy flag, independent of individual engine features. */
    fun darkeningSupported(): Boolean = Build.VERSION.SDK_INT >= 29
}
