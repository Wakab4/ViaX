package moe.pure.browser.web

import moe.pure.browser.ui.message.BrowserMessages

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import moe.pure.browser.core.AddressResolver
import moe.pure.browser.R
import moe.pure.browser.web.external.NativeExternalIntentFactory
import moe.pure.browser.web.permissions.ExternalAppRequest

class BrowserExternalActions(private val activity: Activity,
    private val confirmExternal: (ExternalAppRequest, (Boolean) -> Unit) -> Unit) {
    private val uploads = moe.pure.browser.web.upload.WebUploadController(activity)
    private val sharing = moe.pure.browser.web.external.PageSharing(activity)
    private val otherApps = moe.pure.browser.web.external.OtherAppLauncher(activity)

    fun handleNavigation(url: String, origin: String): Boolean {
        if (AddressResolver.isWebUrl(url)) return false
        val scheme = Uri.parse(url).scheme?.lowercase() ?: return false
        if (scheme in setOf("about", "data", "blob", "javascript", "view-source", "file", "content")) return false
        if (NativeExternalIntentFactory.ignored(url)) return true
        val intent = NativeExternalIntentFactory.create(url) ?: return false
        confirmExternal(ExternalAppRequest(origin, url, intent)) { allowed -> if (allowed) openNative(intent) }
        return true
    }

    private fun openNative(intent: Intent) {
        if (activity.isFinishing || activity.isDestroyed) return
        try { activity.startActivity(intent) }
        catch (_: Exception) { BrowserMessages.show(activity, R.string.ref_external_app_failed) }
    }

    fun chooseFile(callback: ValueCallback<Array<Uri>>, params: WebChromeClient.FileChooserParams): Boolean {
        return uploads.show(callback, params)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        uploads.result(requestCode, resultCode, data)
    }

    fun share(url: String) = sharing.text(url)

    fun sharePage(url: String?, title: String?) = sharing.page(url, title)

    fun openWith(url: String) = otherApps.open(url)

    fun openMedia(url: String, title: String? = null) {
        if (!AddressResolver.isWebUrl(url)) return
        moe.pure.browser.feature.media.external.ExternalVideoPlayer(activity).open(url, title)
    }

    fun dispose() {
        uploads.dispose()
    }

}
