package moe.pure.browser.feature.download.directory

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import android.content.res.Configuration
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import moe.pure.browser.R
import moe.pure.browser.feature.download.workflow.DownloadContinuation
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope
import java.lang.ref.WeakReference

/** Production directory launcher and its small native dialogs share one restorable state owner. */
class DownloadDirectoryFragment : Fragment() {
    interface Host {
        fun ready() = true
        fun directoryResolved(work: DownloadContinuation, changed: Boolean)
    }
    private lateinit var model: DownloadDirectoryModel
    private val main = Handler(Looper.getMainLooper())
    private val dialogs = DialogScope()
    private var host: WeakReference<Host>? = null
    private var shown: DirectoryPhase? = null
    private var dialog: BrowserDialog? = null
    private val draw = Runnable { render() }
    private val picker = registerForActivityResult(NativeDownloadDirectoryContract()) { model.picked(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = DownloadDirectoryModel(requireActivity().application) as T
        })[DownloadDirectoryModel::class.java]
        model.initialize(savedInstanceState?.getBundle(STATE))
        model.state.observe(this) { schedule() }
    }
    internal fun bind(value: Host) { host = WeakReference(value); schedule() }
    internal fun release(value: Host) { if (host?.get() === value) { host = null; dismissWindow() } }
    internal fun begin(work: DownloadContinuation, explain: Boolean) = model.begin(work, explain)
    private fun schedule() { main.removeCallbacks(draw); main.post(draw) }
    private fun render() {
        if (!isAdded || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || parentFragmentManager.isStateSaved) return
        if (parentFragment?.isVisible == false) return
        val observer = host?.get() ?: return
        if (!observer.ready()) return
        val value = model.state.value ?: return
        if (value.phase !in setOf(DirectoryPhase.OPTIONS, DirectoryPhase.EXPLANATION)) dismissWindow()
        model.consume()?.let { outcome ->
            outcome.work?.let { observer.directoryResolved(it, outcome.phase == DirectoryPhase.CHANGED) }
            return
        }
        if (value.phase == DirectoryPhase.PICKER) {
            if (model.launchPicker()) try { picker.launch(value.initialUri?.let(Uri::parse)) } catch (error: Exception) { model.failed(error) }
            return
        }
        if (value.phase !in setOf(DirectoryPhase.OPTIONS, DirectoryPhase.EXPLANATION) || shown == value.phase && dialog?.isShowing == true) return
        dismissWindow()
        val window = if (value.phase == DirectoryPhase.OPTIONS) {
            BrowserDialog(requireActivity(), getString(R.string.ref_download_directory_title))
                .items(listOf(getString(R.string.ref_download_directory_default), getString(R.string.ref_download_directory_choose))) {
                    if (it == 0) model.useDefault() else model.pick()
                }
        } else {
            BrowserDialog(requireActivity(), getString(R.string.ref_download_directory_permission_title), getString(R.string.ref_download_directory_permission_body))
                .positive(getString(android.R.string.ok)) { model.pick(); true }
                .negative(getString(android.R.string.cancel)) { model.cancel(); true }
        }
        window.setOnCancelListener { model.cancel() }
        dialog = window; shown = value.phase
        dialogs.show(window) { if (dialog === window) { dialog = null; shown = null } }
    }
    private fun dismissWindow() { val old = dialog; dialog = null; shown = null; old?.dismiss() }
    override fun onResume() { super.onResume(); schedule() }
    override fun onConfigurationChanged(newConfig: Configuration) { super.onConfigurationChanged(newConfig); dialogs.updateGeometry() }
    override fun onStop() { dismissWindow(); super.onStop() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putBundle(STATE, model.save()); super.onSaveInstanceState(outState) }
    override fun onDestroy() { host = null; main.removeCallbacks(draw); dialogs.dispose(); super.onDestroy() }

    companion object {
        const val SETTINGS = "download_directory_settings"
        const val REQUESTS = "download_directory_requests"
        const val TASKS = "download_directory_tasks"
        private const val STATE = "directory_state"
        internal fun existing(activity: FragmentActivity, tag: String) = existing(activity.supportFragmentManager, tag)
        internal fun existing(manager: FragmentManager, tag: String) = manager.findFragmentByTag(tag) as? DownloadDirectoryFragment
        internal fun obtain(activity: FragmentActivity, tag: String): DownloadDirectoryFragment {
            return obtain(activity.supportFragmentManager, tag)
        }
        internal fun obtain(manager: FragmentManager, tag: String): DownloadDirectoryFragment {
            existing(manager, tag)?.let { return it }
            return DownloadDirectoryFragment().also { manager.beginTransaction().add(it, tag).commitNow() }
        }
    }
}
