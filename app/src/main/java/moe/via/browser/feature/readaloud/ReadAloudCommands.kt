package moe.via.browser.feature.readaloud

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import moe.via.browser.core.logging.NativeLog as Log

/** UI and notification actions enter through the foreground service. */
object ReadAloudCommands {
    const val PLAY = "moe.via.browser.readaloud.PLAY"
    const val PAUSE = "moe.via.browser.readaloud.PAUSE"
    const val STOP = "moe.via.browser.readaloud.STOP"
    const val OPEN_PANEL = "moe.via.browser.READ_ALOUD"

    fun play(context: Context, taskId: String, segmentIndex: Int? = null): Boolean {
        if (taskId.isEmpty()) return false
        return send(context, PLAY, taskId, segmentIndex)
    }

    fun pause(context: Context, taskId: String? = null): Boolean = send(context, PAUSE, taskId)
    fun stop(context: Context): Boolean = send(context, STOP)

    internal fun forward(context: Context, intent: Intent): Boolean {
        val action = intent.action?.takeIf { it == PLAY || it == PAUSE || it == STOP } ?: return false
        return send(context, action, intent.getStringExtra("id"), if (intent.hasExtra("index")) intent.getIntExtra("index", 0) else null)
    }

    private fun send(context: Context, action: String, id: String? = null, index: Int? = null): Boolean {
        val intent = Intent(context, ReadAloudService::class.java).setAction(action)
        if (id != null) intent.putExtra("id", id)
        if (index != null) intent.putExtra("index", index)
        return try {
            context.applicationContext.startForegroundService(intent)
            true
        } catch (error: RuntimeException) {
            Log.warn("ReadAloud", "Cannot start read-aloud service", error)
            // A blocked service start must still allow the user to silence an existing task.
            val main = android.os.Handler(android.os.Looper.getMainLooper())
            main.post {
                val manager = ReadAloudManager.get(context)
                if (action == PAUSE) manager.pause(id)
                if (action == STOP) {
                    manager.stop()
                    context.getSystemService(NotificationManager::class.java).cancel(ReadAloudNotification.ID)
                }
            }
            false
        }
    }
}
