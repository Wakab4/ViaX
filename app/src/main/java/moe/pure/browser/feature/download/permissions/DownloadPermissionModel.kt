package moe.pure.browser.feature.download.permissions

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.download.DownloadPayloads
import moe.pure.browser.feature.download.workflow.DownloadContinuation

internal data class StorageDownloadResult(val request: DownloadContinuation, val allowed: Boolean)

/** Pure pending values survive a Fragment/window replacement; no View, Activity or callback is retained. */
internal class DownloadPermissionModel(application: Application) : AndroidViewModel(application) {
    private var initialized = false
    private var pending: DownloadContinuation? = null
    private var result: StorageDownloadResult? = null
    var storageLaunched = false
    var notificationLaunched = false

    fun initialize(saved: Bundle?) {
        if (initialized) return
        initialized = true
        pending = DownloadContinuation.read(saved?.getBundle("pending"))
        DownloadContinuation.read(saved?.getBundle("result"))?.let { result = StorageDownloadResult(it, saved?.getBoolean("allowed") == true) }
        storageLaunched = saved?.getBoolean("storage_launched") == true
        notificationLaunched = saved?.getBoolean("notification_launched") == true
    }

    fun request(value: DownloadContinuation) {
        val abandoned = listOfNotNull(pending?.payload(), result?.request?.payload())
        pending = value; result = null
        abandoned.filter { it != value.payload() }.distinct().forEach(::discard)
    }
    fun complete(allowed: Boolean) {
        storageLaunched = false
        pending?.let { result = StorageDownloadResult(it, allowed) }
        pending = null
    }
    fun consume(): StorageDownloadResult? = result.also { result = null }
    fun save() = Bundle().apply {
        pending?.let { putBundle("pending", it.save()) }
        result?.let { putBundle("result", it.request.save()); putBoolean("allowed", it.allowed) }
        putBoolean("storage_launched", storageLaunched); putBoolean("notification_launched", notificationLaunched)
    }
    private fun discard(token: String) {
        val app = getApplication<Application>().applicationContext
        BrowserExecutors.persistence.execute { DownloadPayloads.get(app).removeUnused(token) }
    }
    override fun onCleared() {
        listOfNotNull(pending?.payload(), result?.request?.payload()).distinct().forEach(::discard)
        pending = null; result = null
    }
}
