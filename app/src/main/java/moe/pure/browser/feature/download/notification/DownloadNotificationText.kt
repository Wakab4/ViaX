package moe.pure.browser.feature.download.notification

import android.content.Context
import moe.pure.browser.R
import moe.pure.browser.feature.download.DownloadNames
import moe.pure.browser.feature.download.DownloadState
import moe.pure.browser.feature.download.DownloadTask

/** Native notification speed zero is N/A, unlike the list row's zero-size-per-second text. */
internal object DownloadNotificationText {
    fun body(context: Context, task: DownloadTask): String {
        if (task.state == DownloadState.COMPLETE) return context.getString(R.string.ref_download_notification_complete)
        if (task.state == DownloadState.FAILED) return if (task.error.isNullOrEmpty()) context.getString(R.string.ref_download_ui_failed)
            else context.getString(R.string.ref_download_ui_failed_detail, task.error)
        val size = if (task.total <= 0) context.getString(R.string.ref_download_ui_size_only, DownloadNames.size(task.received))
            else context.getString(R.string.ref_download_ui_size_total, DownloadNames.size(task.received), DownloadNames.size(task.total))
        if (task.state == DownloadState.PAUSED || task.state == DownloadState.WAITING_NETWORK) return context.getString(R.string.ref_download_ui_size_status,
            size, context.getString(if (task.state == DownloadState.PAUSED) R.string.ref_download_ui_paused else R.string.ref_download_ui_waiting_network))
        val speed = if (task.speed <= 0) context.getString(R.string.ref_download_ui_not_available) else DownloadNames.size(task.speed) + "/s"
        return if (task.total > 0) context.getString(R.string.ref_download_ui_size_speed_time, size, speed, remaining(context, task))
            else context.getString(R.string.ref_download_ui_size_speed, size, speed)
    }
    private fun remaining(context: Context, task: DownloadTask): String {
        val unknown = context.getString(R.string.ref_download_ui_unknown_time)
        if (task.total <= 0 || task.speed <= 0) return unknown
        val seconds = ((task.total - task.received) / task.speed).toInt()
        val amount: Int
        val resource: Int
        if (seconds <= 60) { amount = seconds; resource = R.plurals.ref_download_eta_seconds }
        else if (seconds / 60 <= 120) { amount = seconds / 60; resource = R.plurals.ref_download_eta_minutes }
        else {
            amount = seconds / 3600
            if (amount > 12) return unknown
            resource = R.plurals.ref_download_eta_hours
        }
        return context.resources.getQuantityString(resource, amount, amount)
    }
}
