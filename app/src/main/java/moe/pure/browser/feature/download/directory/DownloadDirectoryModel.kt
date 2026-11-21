package moe.pure.browser.feature.download.directory

import android.app.Application
import android.net.Uri
import android.os.Bundle
import moe.pure.browser.core.logging.NativeLog as Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.download.DownloadDirectories
import moe.pure.browser.feature.download.DownloadPayloads
import moe.pure.browser.feature.download.workflow.DownloadContinuation

internal enum class DirectoryPhase { IDLE, OPTIONS, EXPLANATION, PICKER, CHANGED, CANCELLED, FAILED }
internal data class DirectoryState(val work: DownloadContinuation? = null, val phase: DirectoryPhase = DirectoryPhase.IDLE,
    val initialUri: String? = null, val launched: Boolean = false)

/** Owns continuation values and directory-result side effects, never an Activity, View or UI callback. */
internal class DownloadDirectoryModel(application: Application) : AndroidViewModel(application) {
    private val app = application.applicationContext
    private val directories by lazy { DownloadDirectories(app) }
    private val updates = MutableLiveData(DirectoryState())
    val state: LiveData<DirectoryState> = updates
    private var value = DirectoryState()
    private var initialized = false

    fun initialize(saved: Bundle?) {
        if (initialized) return
        initialized = true
        val work = DownloadContinuation.read(saved?.getBundle("work"))
        val phase = runCatching { DirectoryPhase.valueOf(saved?.getString("phase").orEmpty()) }.getOrDefault(DirectoryPhase.IDLE)
        publish(if (work == null) DirectoryState() else DirectoryState(work, phase, saved?.getString("initial"), saved?.getBoolean("launched") == true))
    }
    fun begin(work: DownloadContinuation, explain: Boolean) {
        value.work?.payload()?.takeIf { it != work.payload() }?.let(::discard)
        if (value.phase == DirectoryPhase.PICKER && value.launched) { publish(value.copy(work = work)); return }
        publish(DirectoryState(work, if (explain) DirectoryPhase.EXPLANATION else if (directories.isDefault) DirectoryPhase.PICKER else DirectoryPhase.OPTIONS,
            directories.tree?.toString()))
    }
    fun pick() {
        if (value.work == null) return
        val initial = if (value.work == DownloadContinuation.Settings) directories.tree?.toString() else value.initialUri
        publish(value.copy(phase = DirectoryPhase.PICKER, initialUri = initial, launched = false))
    }
    fun useDefault() { directories.useDefault(); publish(value.copy(phase = DirectoryPhase.CHANGED)) }
    fun launchPicker(): Boolean {
        if (value.phase != DirectoryPhase.PICKER || value.launched) return false
        publish(value.copy(launched = true)); return true
    }
    fun picked(uri: Uri?) {
        if (value.work == null || value.phase != DirectoryPhase.PICKER) return
        if (uri == null) { cancel(); return }
        try { directories.select(uri); publish(value.copy(phase = DirectoryPhase.CHANGED, launched = false)) }
        catch (error: Exception) { failed(error) }
    }
    fun failed(error: Exception) {
        Log.warn("DownloadDirectory", "Directory selection failed", error)
        publish(value.copy(phase = DirectoryPhase.FAILED, launched = false))
    }
    fun cancel() { publish(value.copy(phase = DirectoryPhase.CANCELLED, launched = false)) }
    fun consume(): DirectoryState? {
        if (value.phase !in setOf(DirectoryPhase.CHANGED, DirectoryPhase.CANCELLED, DirectoryPhase.FAILED)) return null
        return value.also { publish(DirectoryState()) }
    }
    fun save() = Bundle().apply {
        value.work?.let { putBundle("work", it.save()) }; putString("phase", value.phase.name)
        putString("initial", value.initialUri); putBoolean("launched", value.launched)
    }
    private fun publish(next: DirectoryState) { value = next; updates.value = next }
    private fun discard(token: String) { BrowserExecutors.persistence.execute { DownloadPayloads.get(app).removeUnused(token) } }
    override fun onCleared() { value.work?.payload()?.let(::discard); value = DirectoryState(); super.onCleared() }
}
