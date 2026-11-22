package moe.via.browser.feature.download.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Native task controls are explicit internal broadcasts, then an ordinary service command. */
class DownloadCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (DownloadServiceCommands.accepts(action)) DownloadServiceCommands.send(context.applicationContext, action,
            intent.getLongExtra(DownloadServiceCommands.ID, 0), foreground = false)
    }
}
