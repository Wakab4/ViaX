package moe.pure.browser.feature.customtabs

import android.content.Context
import android.content.Intent
import moe.pure.browser.BrowserActivity
import moe.pure.browser.feature.settings.data.BrowserPreferences

object CustomTabsEntry {
    fun isSessionLaunch(context: Context, intent: Intent?): Boolean = intent != null &&
        runCatching { intent.hasExtra(CustomTabsContract.SESSION) }.getOrDefault(false) &&
        !BrowserPreferences(context.getSharedPreferences("browser", Context.MODE_PRIVATE)).customTabsDisabled

    fun destination(context: Context, incoming: Intent): Intent = Intent(incoming).apply {
        if (isSessionLaunch(context, incoming)) setClass(context, CustomTabActivity::class.java)
        else {
            removeFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            setClass(context, BrowserActivity::class.java)
        }
    }
}
