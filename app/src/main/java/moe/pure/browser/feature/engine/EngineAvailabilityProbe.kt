package moe.pure.browser.feature.engine

import android.webkit.WebView

enum class EngineProviderStatus { AVAILABLE, MISSING, PREPARATION_FAILED }

data class EngineAvailability(
    val status: EngineProviderStatus,
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Long = 0,
) {
    val available get() = status == EngineProviderStatus.AVAILABLE
}

/** Metadata lookup only. This never constructs a WebView or asks the provider to initialize. */
object EngineAvailabilityProbe {
    fun current(): EngineAvailability = try {
        val info = WebView.getCurrentWebViewPackage()
        if (info == null || info.packageName.isNullOrEmpty()) EngineAvailability(EngineProviderStatus.MISSING)
        else EngineAvailability(EngineProviderStatus.AVAILABLE, info.packageName, info.versionName, info.longVersionCode)
    } catch (_: Throwable) {
        EngineAvailability(EngineProviderStatus.PREPARATION_FAILED)
    }
}
