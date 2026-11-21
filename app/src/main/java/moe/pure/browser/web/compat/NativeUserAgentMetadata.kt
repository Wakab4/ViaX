package moe.pure.browser.web.compat

import android.webkit.WebSettings
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/** Via e8.i.O keeps Client Hints aligned with a non-Android user agent. */
internal object NativeUserAgentMetadata {
    fun apply(settings: WebSettings) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) return
        val userAgent = settings.userAgentString ?: return
        if (userAgent.isEmpty() || userAgent.contains("Android")) return

        val current = WebSettingsCompat.getUserAgentMetadata(settings)
        val platform = when {
            userAgent.contains("Windows") -> "Windows"
            userAgent.contains("iPhone") || userAgent.contains("iPad") -> "iOS"
            userAgent.contains("Macintosh") -> "macOS"
            else -> "Unknown"
        }
        val mobile = userAgent.contains("iPhone") || userAgent.contains("iPad")
        WebSettingsCompat.setUserAgentMetadata(settings, UserAgentMetadata.Builder()
            .setPlatform(platform)
            .setPlatformVersion(current.platformVersion)
            .setFullVersion(current.fullVersion?.trim()?.takeIf(String::isNotEmpty))
            .setArchitecture(current.architecture)
            .setModel(current.model)
            .setBitness(current.bitness)
            .setWow64(current.isWow64)
            .setMobile(mobile)
            .setBrandVersionList(current.brandVersionList.filterNot { it.brand.contains("Android") })
            .build())
    }
}
