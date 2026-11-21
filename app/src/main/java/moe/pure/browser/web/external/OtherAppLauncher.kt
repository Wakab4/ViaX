package moe.pure.browser.web.external

import moe.pure.browser.ui.message.BrowserMessages

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Parcelable
import moe.pure.browser.R
import moe.pure.browser.core.BrowserInternalPages
import java.net.URISyntaxException

/** Native z8.f1.f lists matching packages explicitly, leaving this browser out of the result. */
internal class OtherAppLauncher(private val activity: Activity) {
    fun open(url: String) {
        val intent = destination(url)
        if (intent == null) { unavailable(); return }
        try { activity.startActivity(intent) }
        catch (_: ActivityNotFoundException) { unavailable() }
        catch (_: SecurityException) { unavailable() }
    }

    @Suppress("DEPRECATION")
    private fun destination(url: String): Intent? {
        if (url.isEmpty() || BrowserInternalPages.isInternal(activity, url)) return null
        val original = try { Intent.parseUri(url, Intent.URI_INTENT_SCHEME) } catch (_: URISyntaxException) { return null }
        original.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        original.addCategory(Intent.CATEGORY_BROWSABLE)
        val candidates = activity.packageManager.queryIntentActivities(original, 0).mapNotNull { result ->
            val name = result.activityInfo?.packageName ?: return@mapNotNull null
            if (name == activity.packageName) null else Intent(original).setPackage(name)
        }.toMutableList()
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()
        return Intent.createChooser(candidates.removeAt(0), activity.getString(R.string.ref_external_open_title)).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, candidates.map { it as Parcelable }.toTypedArray())
        }
    }

    private fun unavailable() = BrowserMessages.show(activity, R.string.ref_external_unavailable)
}
