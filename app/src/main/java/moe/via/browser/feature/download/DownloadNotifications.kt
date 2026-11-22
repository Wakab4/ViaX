package moe.via.browser.feature.download

import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import moe.via.browser.feature.download.notification.DownloadNotificationFactory
import moe.via.browser.feature.download.model.DownloadState
import moe.via.browser.feature.download.model.DownloadTask

/** Process-wide Native records retain original when/kind/throttle state across service replacements. */
class DownloadNotifications private constructor(context: Context) {
    private data class Record(val notificationId: Int, val createdWall: Long, var lastPost: Long = 0, var kind: Int = 0)
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val factory = DownloadNotificationFactory(context)
    private val records = mutableMapOf<Long, Record>()
    fun channels() = factory.channels(manager)
    fun foreground() = factory.foreground()
    @Synchronized fun show(task: DownloadTask) {
        if (task.state == DownloadState.CANCELED) { remove(task.id); return }
        val kind = when (task.state) {
            DownloadState.COMPLETE -> 1
            DownloadState.FAILED -> 2
            DownloadState.WAITING_NETWORK -> 3
            DownloadState.PAUSED -> 5
            else -> 4
        }
        val now = SystemClock.elapsedRealtime()
        val existing = records[task.id]
        if (existing != null && existing.kind == kind && now - existing.lastPost < 1000) return
        val record = existing ?: Record((task.id xor (task.id ushr 32)).toInt(), System.currentTimeMillis()).also { records[task.id] = it }
        record.kind = kind; record.lastPost = now
        if (kind == 1 || kind == 2) manager.cancel(record.notificationId)
        manager.notify(record.notificationId, factory.task(task, record.createdWall))
    }
    @Synchronized fun remove(id: Long) { records.remove(id)?.let { manager.cancel(it.notificationId) } }
    companion object {
        const val FOREGROUND_ID = 1
        @Volatile private var current: DownloadNotifications? = null
        fun get(context: Context): DownloadNotifications = current ?: synchronized(this) {
            current ?: DownloadNotifications(context.applicationContext).also { current = it }
        }
    }
}
