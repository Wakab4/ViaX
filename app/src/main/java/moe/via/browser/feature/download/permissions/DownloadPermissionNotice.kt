package moe.via.browser.feature.download.permissions

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import moe.via.browser.R
import moe.via.browser.ui.message.MessageContent
import moe.via.browser.ui.message.MessageOwner

/** z8.z1.f: denial offers application settings through the original action message. */
internal class DownloadPermissionNotice(private val activity: Activity) : AutoCloseable {
    private val message = MessageOwner(activity)
    fun show() {
        message.show(MessageContent(activity.getString(R.string.ref_download_permission_denied), activity.getString(android.R.string.ok), clicked = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${activity.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            runCatching { activity.startActivity(intent) }
        }))
    }
    override fun close() = message.close(false)
}
