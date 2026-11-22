package moe.via.browser.feature.readaloud

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import moe.via.browser.BrowserActivity
import moe.via.browser.R

/** The reference uses a standard low-priority notification with one pause/resume action. */
class ReadAloudNotification(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)
    init {
        manager.deleteNotificationChannel("${context.packageName}.CHANNEL.READ_ALOUD")
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "朗读网页", NotificationManager.IMPORTANCE_LOW))
    }

    fun build(task: ReadAloudTask?): Notification {
        val playing = task?.playing == true
        val open = Intent(context, BrowserActivity::class.java).setAction(ReadAloudCommands.OPEN_PANEL)
            .setPackage(context.packageName).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val action = Intent(context, ReadAloudReceiver::class.java)
            .setAction(if (playing) ReadAloudCommands.PAUSE else ReadAloudCommands.PLAY)
        task?.let { action.putExtra("id", it.id) }
        return Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ref_tts_notification_small)
            .setContentTitle(task?.title?.takeIf(String::isNotBlank) ?: "无标题")
            .setContentText(domain(task?.url.orEmpty()))
            .setCategory(if (playing) Notification.CATEGORY_PROGRESS else Notification.CATEGORY_STATUS)
            .setOngoing(playing)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(PendingIntent.getActivity(context, 0, open, FLAGS))
            .addAction(Notification.Action.Builder(
                if (playing) R.drawable.ref_tts_notification_pause else R.drawable.ref_tts_notification_resume,
                if (playing) "暂停" else "恢复", PendingIntent.getBroadcast(context, 0, action, FLAGS)
            ).build())
            .build()
    }

    fun update(task: ReadAloudTask?) { runCatching { manager.notify(ID, build(task)) } }
    fun cancel() = manager.cancel(ID)

    private fun domain(url: String): String {
        var host = runCatching { Uri.parse(url).host }.getOrNull().orEmpty()
        val prefix = when { host.startsWith("www.") -> "www."; host.startsWith("m.") -> "m."; else -> "" }
        if (prefix.isNotEmpty() && host.substring(prefix.length).contains('.')) host = host.substring(prefix.length)
        return host
    }

    companion object {
        const val ID = 2
        private const val CHANNEL = "moe.via.browser.CHANNEL.READ_ALOUD2"
        private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }
}
