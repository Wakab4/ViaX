package moe.via.browser.feature.customtabs

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build

/** Native sends only the current URL as fill-in data, on an explicit toolbar/menu click. */
internal object CustomTabPendingAction {
    fun send(activity: Activity, pending: PendingIntent?, url: String) {
        if (pending == null) return
        val fillIn = url.takeIf(String::isNotEmpty)?.let { Intent().setData(Uri.parse(it)) }
        runCatching {
            if (Build.VERSION.SDK_INT < 34) pending.send(activity, 0, fillIn)
            else {
                val options = ActivityOptions.makeBasic().apply {
                    setPendingIntentBackgroundActivityStartMode(if (Build.VERSION.SDK_INT >= 36)
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS else ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                }
                pending.send(activity, 0, fillIn, null, null, null, options.toBundle())
            }
        }
    }
}
