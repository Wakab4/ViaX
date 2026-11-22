package moe.via.browser.app.lifecycle

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import moe.via.browser.BrowserActivity
import kotlin.system.exitProcess

/** z8.f.k starts the launcher task and exits the old process. */
internal object NativeAppRestart {
    fun restart(context: Context): Nothing {
        context.startActivity(Intent.makeRestartActivityTask(ComponentName(context, BrowserActivity::class.java))
            .setPackage(context.packageName))
        exitProcess(0)
    }
}
