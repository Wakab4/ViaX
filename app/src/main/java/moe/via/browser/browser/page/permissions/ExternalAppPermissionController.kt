package moe.via.browser.browser.page.permissions

import android.app.Activity
import android.content.pm.PackageManager
import moe.via.browser.storage.preferences.BrowserSettings
import moe.via.browser.feature.site.SiteDecision
import moe.via.browser.feature.site.SiteFeature
import moe.via.browser.feature.site.data.SiteSettingsRepository

/** s6.J9 / ua.P0: an external-app decision is separate from WebRTC permissions. */
internal class ExternalAppPermissionController(
    private val activity: Activity,
    private val settings: BrowserSettings,
    private val sites: SiteSettingsRepository,
    private val canPresent: () -> Boolean,
    private val currentOrigin: (() -> String?)?
) {
    private val prompt = ExternalAppPrompt(activity)
    private var disposed = false

    fun request(request: ExternalAppRequest, result: (Boolean) -> Unit) {
        if (disposed || activity.isFinishing || activity.isDestroyed) { result(false); return }
        // Native reads G8() at dispatch time; an owned old WebView does not replace the selected URL.
        val origin = if (currentOrigin == null) request.origin else currentOrigin.invoke().orEmpty()
        val decision = sites.resolve(origin, SiteFeature.OPEN_APPS, settings)
        if (decision == SiteDecision.DENY || !canPresent()) { result(false); return }
        val label = applicationLabel(request) ?: run { result(false); return }
        if (decision == SiteDecision.ALLOW) result(true)
        else prompt.show(request.targetUrl, label) {
            if (!disposed && !activity.isFinishing && !activity.isDestroyed) result(true)
        }
    }

    @Suppress("DEPRECATION")
    private fun applicationLabel(request: ExternalAppRequest): String? = runCatching {
        val packages = activity.packageManager
        val info = request.intent.resolveActivityInfo(packages, PackageManager.GET_META_DATA) ?: return@runCatching null
        if (info.packageName == "android") "" else packages.getApplicationLabel(info.applicationInfo).toString()
    }.getOrNull()

    fun dispose() {
        if (disposed) return
        disposed = true
        prompt.dispose()
    }
}
