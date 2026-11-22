package moe.via.browser.feature.readaloud

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReadAloudReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReadAloudCommands.forward(context, intent)
    }
}
