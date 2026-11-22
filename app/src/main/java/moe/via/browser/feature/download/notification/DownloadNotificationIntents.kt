package moe.via.browser.feature.download.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import moe.via.browser.feature.download.model.DownloadTask
import moe.via.browser.feature.download.actions.DownloadFileIntents
import moe.via.browser.feature.download.service.DownloadCommandReceiver
import moe.via.browser.feature.download.service.DownloadServiceCommands
import moe.via.browser.BrowserActivity
import moe.via.browser.feature.download.ui.DownloadPageIntents

/** Native requestCode0/update-current/immutable identities are preserved, including action sharing. */
internal class DownloadNotificationIntents(private val context: Context) {
    private val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    fun manager(): PendingIntent = PendingIntent.getActivity(context, 0,
        Intent(context, BrowserActivity::class.java).setAction(DownloadPageIntents.ACTION)
            .setPackage(context.packageName), flags)
    fun control(task: DownloadTask, active: Boolean): PendingIntent = PendingIntent.getBroadcast(context, 0,
        Intent(context, DownloadCommandReceiver::class.java).setAction(if (active) DownloadServiceCommands.PAUSE else DownloadServiceCommands.RESUME)
            .putExtra(DownloadServiceCommands.ID, task.id), flags)
    fun completed(task: DownloadTask): PendingIntent {
        val intent = DownloadFileIntents.view(context, task)
        return PendingIntent.getActivity(context, 0, intent, flags)
    }
}
