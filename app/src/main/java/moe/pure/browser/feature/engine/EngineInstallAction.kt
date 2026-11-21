package moe.pure.browser.feature.engine

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import moe.pure.browser.R

/** Exact GP z8.f.g market destination and country-specific external-browser fallback. */
internal object EngineInstallAction {
    fun open(activity: Activity) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE")).apply {
            component = null; selector = null
        }
        try { activity.startActivity(market) }
        catch (_: ActivityNotFoundException) {
            val prefix = if (activity.resources.configuration.locales[0].country == "CN")
                "http://coolapk.com/apk/" else "https://play.google.com/store/apps/details?id="
            val browser = Intent(Intent.ACTION_VIEW, Uri.parse(prefix + PACKAGE))
            activity.startActivity(Intent.createChooser(browser, activity.getString(R.string.ref_engine_open_with)))
        }
    }

    private const val PACKAGE = "com.google.android.webview"
}
