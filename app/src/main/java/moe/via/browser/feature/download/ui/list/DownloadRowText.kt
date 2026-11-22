package moe.via.browser.feature.download.ui.list

import android.content.Context
import moe.via.browser.R
import moe.via.browser.feature.download.DownloadNames
import moe.via.browser.feature.download.model.DownloadState
import moe.via.browser.feature.download.model.DownloadTask

/** DownloadTaskViewDelegate.A/r: display formatting does not alter queue state. */
internal object DownloadRowText {
    fun running(task: DownloadTask) = task.state == DownloadState.RUNNING ||
        task.state == DownloadState.QUEUED || task.state == DownloadState.PUBLISHING || task.state == DownloadState.WAITING_NETWORK

    fun description(context: Context, task: DownloadTask): String? {
        if (task.state == DownloadState.COMPLETE) return DownloadNames.size(maxOf(task.received, task.total))
        if (task.state == DownloadState.FAILED) return if (task.error.isNullOrEmpty())
            context.getString(R.string.ref_download_ui_failed)
        else context.getString(R.string.ref_download_ui_failed_detail, task.error)
        if (!running(task) && task.state != DownloadState.PAUSED) return null
        val received = DownloadNames.size(task.received)
        val size = if (task.total <= 0) context.getString(R.string.ref_download_ui_size_only, received)
        else context.getString(R.string.ref_download_ui_size_total, received, DownloadNames.size(task.total))
        if (task.state == DownloadState.PAUSED) return context.getString(R.string.ref_download_ui_size_status,
            size, context.getString(R.string.ref_download_ui_paused))
        if (task.state == DownloadState.WAITING_NETWORK) return context.getString(R.string.ref_download_ui_size_status,
            size, context.getString(R.string.ref_download_ui_waiting_network))
        val speed = if (task.speed < 0) context.getString(R.string.ref_download_ui_not_available)
        else DownloadNames.size(task.speed) + "/s"
        return if (task.total > 0) context.getString(R.string.ref_download_ui_size_speed_time, size, speed, remaining(context, task))
        else context.getString(R.string.ref_download_ui_size_speed, size, speed)
    }

    private fun remaining(context: Context, task: DownloadTask): String {
        val unknown = context.getString(R.string.ref_download_ui_unknown_time)
        if (task.total <= 0 || task.speed <= 0) return unknown
        val seconds = ((task.total - task.received) / task.speed).toInt()
        val quantity: Int
        val resource: Int
        if (seconds <= 60) { quantity = seconds; resource = R.plurals.ref_download_eta_seconds }
        else {
            val minutes = seconds / 60
            if (minutes <= 120) { quantity = minutes; resource = R.plurals.ref_download_eta_minutes }
            else {
                val hours = minutes / 60
                if (hours > 12) return unknown
                quantity = hours; resource = R.plurals.ref_download_eta_hours
            }
        }
        return context.resources.getQuantityString(resource, quantity, quantity)
    }
}
