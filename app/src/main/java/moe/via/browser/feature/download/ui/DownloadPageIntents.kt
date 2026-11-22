package moe.via.browser.feature.download.ui

import android.content.Intent

/** DOWNLOADER is dispatched through the browser window, matching the native entry action. */
object DownloadPageIntents {
    const val ACTION = "moe.via.browser.DOWNLOADER"
    fun matches(intent: Intent): Boolean = intent.action.equals(ACTION, true)
    fun consume(intent: Intent) {
        if (matches(intent)) intent.action = Intent.ACTION_MAIN
    }
}
