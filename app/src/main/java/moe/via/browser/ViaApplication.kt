package moe.via.browser

import android.app.Application
import moe.via.browser.core.logging.NativeLog
import moe.via.browser.app.lifecycle.NativeLogVersion
import moe.via.browser.app.lifecycle.NativeApplicationErrors
import moe.via.browser.feature.proxy.BrowserProxy
import moe.via.browser.webview.compat.NativeWebViewProcess
import moe.via.browser.feature.settings.data.updates.NativePreferenceUpdates

/** Native application roles keep logging, uncaught handling and WebView process setup separate. */
class ViaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NativeLog.initialize(this)?.let { NativeLogVersion.clearAfterUpgrade(this, it) }
        NativeApplicationErrors.install(this)
        NativeWebViewProcess.configure(this)
        BrowserProxy.initialize(this)
        NativePreferenceUpdates.apply(this)
    }
}
