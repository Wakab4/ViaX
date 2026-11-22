package moe.via.browser.browser.navigation.external

import moe.via.browser.ui.message.BrowserMessages

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import moe.via.browser.R
import moe.via.browser.feature.home.HomeDocument

/** Main-page sharing includes the title in the text; context links share only their original value. */
internal class PageSharing(private val activity: Activity) {
    fun page(url: String?, title: String?) {
        val payload = if (url.isNullOrEmpty() || url.startsWith("file://", true) || HomeDocument.isHome(activity, url)) {
            "http://viayoo.com/"
        } else if (!title.isNullOrEmpty()) "$title\n$url" else url
        text(payload)
    }

    fun text(value: String) {
        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, value) }
        try { activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.ref_external_share_title))) }
        catch (_: ActivityNotFoundException) { unavailable() }
        catch (_: SecurityException) { unavailable() }
    }

    private fun unavailable() = BrowserMessages.show(activity, R.string.ref_external_unavailable)
}
