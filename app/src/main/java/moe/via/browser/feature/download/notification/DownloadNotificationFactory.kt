package moe.via.browser.feature.download.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import moe.via.browser.R
import moe.via.browser.feature.download.model.DownloadState
import moe.via.browser.feature.download.model.DownloadTask
import moe.via.browser.core.localization.SettingsContext

/** mark.via.download.d.g/h and DownloadService.d. Foreground and task builders have distinct surfaces. */
internal class DownloadNotificationFactory(private val app: Context) {
    private val context get() = SettingsContext.wrap(app)
    private val intents = DownloadNotificationIntents(app)
    val foregroundChannel = app.packageName + ".FOREGROUND"
    val taskChannel = app.packageName + ".DOWNLOAD"
    private val group = app.packageName + ".GENERAL_GROUP"

    fun channels(manager: NotificationManager) {
        val localized = context
        manager.createNotificationChannelGroup(NotificationChannelGroup(group, localized.getString(R.string.ref_download_notification_general)))
        manager.deleteNotificationChannel(app.packageName + ".DOWNLOAD_FORGROUND")
        manager.deleteNotificationChannel("downloads")
        manager.deleteNotificationChannel("downloads_completed")
        manager.createNotificationChannels(listOf(
            NotificationChannel(foregroundChannel, localized.getString(R.string.ref_download_notification_foreground_channel), NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) },
            NotificationChannel(taskChannel, localized.getString(R.string.ref_download_notification_channel), NotificationManager.IMPORTANCE_LOW).apply { this.group = this@DownloadNotificationFactory.group }))
    }
    fun foreground(): Notification {
        val localized = context
        return NotificationCompat.Builder(localized, foregroundChannel).setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(intents.manager()).setContentTitle(localized.getString(R.string.ref_download_notification_foreground_title))
            .setContentText(localized.getString(R.string.ref_download_notification_foreground_body))
            .setSilent(true).setCategory(NotificationCompat.CATEGORY_PROGRESS).build()
    }
    fun task(task: DownloadTask, whenCreated: Long): Notification {
        val localized = context
        val active = task.state in setOf(DownloadState.QUEUED, DownloadState.RUNNING, DownloadState.PUBLISHING, DownloadState.WAITING_NETWORK)
        val waiting = task.state == DownloadState.WAITING_NETWORK
        val complete = task.state == DownloadState.COMPLETE
        val failed = task.state == DownloadState.FAILED
        val body = DownloadNotificationText.body(localized, task)
        val builder = NotificationCompat.Builder(localized, taskChannel).setWhen(whenCreated).setContentTitle(task.name)
            .setContentText(body).setOngoing(active && !waiting).setAutoCancel(!active || waiting)
        if (complete || failed) {
            builder.setSmallIcon(if (complete) android.R.drawable.stat_sys_download_done else R.drawable.ref_ic_stat_download_error)
                .setCategory(if (complete) NotificationCompat.CATEGORY_STATUS else NotificationCompat.CATEGORY_ERROR)
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setProgress(100, if (task.total > 0) (task.received.toFloat() / task.total.toFloat() * 100f).toInt() else 50, task.total <= 0)
                .setSmallIcon(if (active && !waiting) android.R.drawable.stat_sys_download else R.drawable.ref_ic_stat_download_pause)
                .setCategory(if (active && !waiting) NotificationCompat.CATEGORY_PROGRESS else NotificationCompat.CATEGORY_STATUS)
                .addAction(NotificationCompat.Action.Builder(if (active) R.drawable.ref_ic_stat_download_pause else R.drawable.ref_ic_stat_download_resume,
                    localized.getString(if (active) R.string.ref_download_notification_pause else R.string.ref_download_notification_resume), intents.control(task, active)).build())
        }
        return builder.setContentIntent(if (complete) intents.completed(task) else intents.manager()).build()
    }
}
