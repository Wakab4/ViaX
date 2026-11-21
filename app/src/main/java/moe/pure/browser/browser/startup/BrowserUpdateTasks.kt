package moe.pure.browser.browser.startup

import android.content.Context
import moe.pure.browser.feature.adblock.AdblockEngine
import moe.pure.browser.feature.adblock.FilterSubscriptionUpdater
import moe.pure.browser.feature.scripts.data.ScriptUpdater

/** Shell.Z loads filters; ua.q0/sb.i starts the two independent update streams. */
internal object BrowserUpdateTasks {
    fun start(context: Context) {
        AdblockEngine.start(context)
        FilterSubscriptionUpdater.get(context).maybeUpdate()
        ScriptUpdater.get(context).maybeUpdate()
    }
}
