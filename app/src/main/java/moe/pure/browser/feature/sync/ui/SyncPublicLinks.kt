package moe.pure.browser.feature.sync.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import moe.pure.browser.BrowserActivity

/** The reference page has automatic registration, not a separate registration or password-reset endpoint. */
internal object SyncPublicLinks {
    fun terms(server: Int) = "https://viayoo.com/${if (server == 1) "zh-cn" else "en"}/docs/terms-of-use.html"
    fun privacy(server: Int) = "https://viayoo.com/${if (server == 1) "zh-cn" else "en"}/docs/privacy-policy.html"
    fun open(activity: Activity, url: String) {
        val uri = Uri.parse(url)
        require(uri.scheme == "https" && uri.host == "viayoo.com")
        activity.startActivity(Intent(activity, BrowserActivity::class.java).setAction(Intent.ACTION_VIEW).setData(uri))
    }
}
