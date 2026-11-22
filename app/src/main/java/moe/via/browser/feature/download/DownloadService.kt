package moe.via.browser.feature.download

import android.app.Service
import android.content.Intent
import android.os.IBinder
import moe.via.browser.feature.download.service.DownloadServiceCommands
import moe.via.browser.feature.download.model.DownloadState
import moe.via.browser.feature.download.model.DownloadTask
import moe.via.browser.feature.download.transfer.DownloadCoordinator

/** Native service owns listeners and foreground presentation; the application queue owns transfers. */
class DownloadService : Service(), DownloadCoordinator.Events {
    private lateinit var queue: DownloadCoordinator
    private lateinit var notifications: DownloadNotifications
    private var ready = false
    private var stopRequested = false
    private var terminalObserved = false
    private var pendingCommands = 0
    private var idleCheckPending = false
    private var destroyed = false
    override fun onCreate() {
        super.onCreate()
        queue = DownloadCoordinator.get(this)
        notifications = DownloadNotifications.get(this)
        queue.observeEvents(this)
        notifications.channels()
        startForeground(DownloadNotifications.FOREGROUND_ID, notifications.foreground())
        ready = true
        if (stopRequested) stopForegroundService()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val id = intent?.getLongExtra(DownloadServiceCommands.ID, 0) ?: 0
        when (DownloadServiceCommands.command(intent)) {
            DownloadServiceCommands.Command.RESUME -> if (id > 0) submit { done -> queue.start(id, false) { done() } }
            DownloadServiceCommands.Command.REDOWNLOAD -> if (id > 0) submit { done -> queue.start(id, true) { done() } }
            DownloadServiceCommands.Command.PAUSE -> if (id > 0) submit { done -> queue.pause(id) { done(); stopIfIdle() } }
            DownloadServiceCommands.Command.PAUSE_ALL -> submit { done -> queue.pauseAll { done(); stopForegroundService() } }
            DownloadServiceCommands.Command.RESUME_PENDING -> submit { done -> queue.resumePending { active -> done(); if (!active) stopIfIdle() } }
            DownloadServiceCommands.Command.NONE -> Unit
        }
        return START_STICKY
    }
    override fun taskChanged(task: DownloadTask, previous: DownloadTask?) {
        if (previous == null && task.state == DownloadState.QUEUED) return
        notifications.show(task)
        if (task.state in setOf(DownloadState.COMPLETE, DownloadState.FAILED, DownloadState.PAUSED, DownloadState.WAITING_NETWORK, DownloadState.CANCELED)) {
            terminalObserved = true; stopIfIdle()
        }
    }
    override fun activityChanged(busy: Boolean) { if (!busy && terminalObserved) stopIfIdle() }
    private fun submit(command: (() -> Unit) -> Unit) {
        pendingCommands++
        command {
            pendingCommands--
            if (!destroyed && pendingCommands == 0 && idleCheckPending) stopIfIdle()
        }
    }
    private fun stopIfIdle() {
        if (destroyed) return
        if (pendingCommands > 0) { idleCheckPending = true; return }
        idleCheckPending = false
        if (!queue.busy) stopForegroundService()
    }
    private fun stopForegroundService() {
        if (destroyed) return
        if (!ready) { stopRequested = true; return }
        try { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
        catch (error: Exception) { moe.via.browser.core.logging.NativeLog.warn("DownloadService", "Cannot stop foreground service", error) }
    }
    override fun onDestroy() { destroyed = true; if (::queue.isInitialized) queue.removeEvents(this); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
