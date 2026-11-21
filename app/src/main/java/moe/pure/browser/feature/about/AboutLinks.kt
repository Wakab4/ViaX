package moe.pure.browser.feature.about

import android.app.Activity
import android.content.Intent
import android.net.Uri
import moe.pure.browser.BrowserActivity

/** Opens license project links in the browser. */
object AboutLinks {
    fun open(activity: Activity, url: String) {
        activity.startActivity(Intent(activity, BrowserActivity::class.java)
            .setAction(Intent.ACTION_VIEW).setData(Uri.parse(url)))
    }
}
