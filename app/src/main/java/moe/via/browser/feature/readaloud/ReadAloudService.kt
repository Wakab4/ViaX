package moe.via.browser.feature.readaloud

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper

class ReadAloudService : Service() {
    private lateinit var manager: ReadAloudManager
    private lateinit var notification: ReadAloudNotification
    private var commandReady = false
    private var ending = false
    private var lastStartId = 0
    private val changed: (ReadAloudTask?) -> Unit = { update(it) }
    private val devices = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) {
            if (removed.isNotEmpty() && !ending) { manager.pause(); update(manager.snapshot()) }
        }
    }
    private val audio by lazy { getSystemService(AudioManager::class.java) }

    override fun onCreate() {
        super.onCreate()
        manager = ReadAloudManager.get(this)
        notification = ReadAloudNotification(this)
        val initial = notification.build(manager.snapshot())
        if (Build.VERSION.SDK_INT >= 29) startForeground(ReadAloudNotification.ID, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(ReadAloudNotification.ID, initial)
        manager.observe(changed)
        audio?.registerAudioDeviceCallback(devices, Handler(Looper.getMainLooper()))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        ending = false
        commandReady = false
        when (intent?.action) {
            ReadAloudCommands.PLAY -> {
                val id = intent.getStringExtra("id")
                if (id != null) manager.play(id, if (intent.hasExtra("index")) intent.getIntExtra("index", 0) else null)
            }
            ReadAloudCommands.PAUSE -> manager.pause(intent.getStringExtra("id"))
            ReadAloudCommands.STOP -> manager.stop()
        }
        commandReady = true
        update(manager.snapshot())
        // Article text is intentionally process-local; a system restart cannot restore it.
        return START_NOT_STICKY
    }

    private fun update(task: ReadAloudTask?) {
        if (ending || !::notification.isInitialized) return
        notification.update(task)
        if (!commandReady) return
        if (task == null || task.completed) finishService(remove = true)
        else if (!task.playing) finishService(remove = false)
    }

    private fun finishService(remove: Boolean) {
        if (ending) return
        if (!stopSelfResult(lastStartId)) return
        ending = true
        stopForeground(if (remove) STOP_FOREGROUND_REMOVE else STOP_FOREGROUND_DETACH)
        if (remove) notification.cancel()
    }

    override fun onDestroy() {
        audio?.unregisterAudioDeviceCallback(devices)
        manager.removeObserver(changed)
        // Never leave speech running after an unexpected service destruction.
        if (!ending && manager.snapshot()?.playing == true) manager.pause()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
