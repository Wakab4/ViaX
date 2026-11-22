package moe.via.browser.feature.download.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import moe.via.browser.feature.download.workflow.DownloadContinuation
import java.lang.ref.WeakReference

/** Native z8.z1 launchers use separate notification and storage results, rather than window closures. */
class DownloadPermissionFragment : Fragment() {
    interface Host {
        fun ready() = true
        fun storageResolved(work: DownloadContinuation, allowed: Boolean)
    }
    private val main = Handler(Looper.getMainLooper())
    private var host: WeakReference<Host>? = null
    private lateinit var model: DownloadPermissionModel
    private val deliver = Runnable { dispatch() }
    private val notifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        model.notificationLaunched = false // Optional permission does not approve or reject a download.
    }
    private val storage = registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        model.complete(allowed); dispatch()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = DownloadPermissionModel(requireActivity().application) as T
        })[DownloadPermissionModel::class.java]
        model.initialize(savedInstanceState?.getBundle(STATE))
    }
    internal fun bind(value: Host) { host = WeakReference(value); main.removeCallbacks(deliver); main.post(deliver) }
    internal fun release(value: Host) { if (host?.get() === value) host = null }
    internal fun requestNotifications() {
        if (Build.VERSION.SDK_INT < 33 || allowed(Manifest.permission.POST_NOTIFICATIONS) || model.notificationLaunched) return
        model.notificationLaunched = true
        try { notifications.launch(Manifest.permission.POST_NOTIFICATIONS) }
        catch (_: Exception) { model.notificationLaunched = false }
    }
    internal fun requestStorage(work: DownloadContinuation) {
        model.request(work)
        if (allowed(Manifest.permission.WRITE_EXTERNAL_STORAGE)) { model.complete(true); dispatch(); return }
        if (model.storageLaunched) return
        model.storageLaunched = true
        try { storage.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) }
        catch (_: Exception) { model.complete(false); dispatch() }
    }
    private fun allowed(permission: String) = requireContext().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    private fun dispatch() {
        if (!isAdded || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || parentFragmentManager.isStateSaved) return
        if (parentFragment?.isVisible == false) return
        val observer = host?.get() ?: return
        if (!observer.ready()) return
        val result = model.consume() ?: return
        observer.storageResolved(result.request, result.allowed)
    }
    override fun onResume() { super.onResume(); dispatch() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putBundle(STATE, model.save()); super.onSaveInstanceState(outState) }
    override fun onDestroy() { host = null; main.removeCallbacks(deliver); super.onDestroy() }

    companion object {
        internal const val REQUESTS = "download_runtime_permissions"
        internal const val TASKS = "download_task_permissions"
        private const val STATE = "download_permission_state"
        internal fun existing(activity: FragmentActivity, tag: String = REQUESTS) = existing(activity.supportFragmentManager, tag)
        internal fun existing(manager: FragmentManager, tag: String = REQUESTS) = manager.findFragmentByTag(tag) as? DownloadPermissionFragment
        internal fun obtain(activity: FragmentActivity, tag: String = REQUESTS): DownloadPermissionFragment {
            return obtain(activity.supportFragmentManager, tag)
        }
        internal fun obtain(manager: FragmentManager, tag: String = REQUESTS): DownloadPermissionFragment {
            existing(manager, tag)?.let { return it }
            return DownloadPermissionFragment().also { manager.beginTransaction().add(it, tag).commitNow() }
        }
    }
}
