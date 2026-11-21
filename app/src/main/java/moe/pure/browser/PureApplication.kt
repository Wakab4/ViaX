package moe.pure.browser

import android.app.Application
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.core.logging.NativeLogVersion
import moe.pure.browser.core.app.NativeApplicationErrors
import moe.pure.browser.web.compat.NativeWebViewProcess
import moe.pure.browser.feature.settings.data.updates.NativePreferenceUpdates

/** Native application roles keep logging, uncaught handling and WebView process setup separate. */
class PureApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NativeLog.initialize(this)?.let { NativeLogVersion.clearAfterUpgrade(this, it) }
        NativeApplicationErrors.install(this)
        NativeWebViewProcess.configure(this)
        NativePreferenceUpdates.apply(this)
    }
}
