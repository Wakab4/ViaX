package moe.via.browser.ui.pages

import android.os.Build
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import moe.via.browser.storage.preferences.BrowserPreferences
import moe.via.browser.ui.theme.ThemedActivity

/** Handles browser back after the current record page's own callback. */
internal class PageBackDispatcher(private val activity: ThemedActivity, private val back: () -> Unit) : AutoCloseable {
    private var webTarget: PredictiveBackPreview? = null
    private val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackStarted(backEvent: BackEventCompat) {
            val unsupported = listOf(Build.BRAND, Build.MANUFACTURER).any { it.equals("HONOR", true) || it.equals("HUAWEI", true) }
            val preferences = BrowserPreferences(activity.getSharedPreferences("browser", 0))
            if (Build.VERSION.SDK_INT < 35 || unsupported || preferences.predictiveBackDisabled) return
            if (activity.currentRecordStack?.top == null) webTarget = activity.beginBrowserPredictiveBack()
        }

        override fun handleOnBackProgressed(backEvent: BackEventCompat) { webTarget?.progress(backEvent.progress) }

        override fun handleOnBackCancelled() { cancel() }

        override fun handleOnBackPressed() {
            val web = webTarget
            webTarget = null
            if (web != null) web.complete()
            else back()
        }
    }

    fun install() { activity.onBackPressedDispatcher.addCallback(callback) }

    fun cancel() {
        webTarget?.cancel(); webTarget = null
    }

    override fun close() { cancel(); callback.remove() }
}
