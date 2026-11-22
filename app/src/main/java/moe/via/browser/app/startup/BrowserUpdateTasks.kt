package moe.via.browser.app.startup

import android.content.Context
import moe.via.browser.feature.adblock.AdblockEngine
import moe.via.browser.feature.adblock.FilterSubscriptionUpdater
import moe.via.browser.feature.scripts.data.ScriptUpdater

/** Shell.Z loads filters; ua.q0/sb.i starts the two independent update streams. */
internal object BrowserUpdateTasks {
    fun start(context: Context) {
        AdblockEngine.start(context)
        FilterSubscriptionUpdater.get(context).maybeUpdate()
        ScriptUpdater.get(context).maybeUpdate()
    }
}
