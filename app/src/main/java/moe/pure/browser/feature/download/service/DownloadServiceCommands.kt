package moe.pure.browser.feature.download.service

import android.content.Context
import android.content.Intent
import moe.pure.browser.core.logging.NativeLog as Log
import moe.pure.browser.feature.download.DownloadService

/** Native action suffixes are preserved, including its counterintuitive ALL command mapping. */
internal object DownloadServiceCommands {
    const val RESUME = "moe.pure.browser.receiver.DownloadReceiver.RESUME"
    const val PAUSE = "moe.pure.browser.receiver.DownloadReceiver.PAUSE"
    const val REDOWNLOAD = "moe.pure.browser.receiver.DownloadReceiver.REDOWNLOAD"
    const val RESUME_ALL = "moe.pure.browser.receiver.DownloadReceiver.RESUME_ALL"
    const val PAUSE_ALL = "moe.pure.browser.receiver.DownloadReceiver.PAUSE_ALL"
    const val ID = "id"
    enum class Command { RESUME, PAUSE, REDOWNLOAD, PAUSE_ALL, RESUME_PENDING, NONE }

    fun command(intent: Intent?): Command = when (intent?.action) {
        RESUME -> Command.RESUME
        PAUSE -> Command.PAUSE
        REDOWNLOAD -> Command.REDOWNLOAD
        RESUME_ALL -> Command.PAUSE_ALL
        PAUSE_ALL -> Command.RESUME_PENDING
        else -> Command.NONE
    }
    fun send(context: Context, action: String, id: Long = 0, foreground: Boolean = true) {
        val intent = Intent(context, DownloadService::class.java).setAction(action).putExtra(ID, id)
        try { if (foreground) context.startForegroundService(intent) else context.startService(intent) }
        catch (error: Exception) { Log.warn("DownloadService", "Cannot dispatch download command", error) }
    }
    fun accepts(action: String?) = action in setOf(RESUME, PAUSE, REDOWNLOAD, RESUME_ALL, PAUSE_ALL)
}
