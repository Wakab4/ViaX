package moe.pure.browser.feature.download

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import moe.pure.browser.feature.download.directory.DownloadDirectoryFragment
import moe.pure.browser.feature.download.workflow.DownloadContinuation

/** Window bridge only; Fragment/VM owns all picker, explanation and option phases. */
class DownloadDirectoryDialog(private val activity: FragmentActivity,
    private val tag: String = DownloadDirectoryFragment.SETTINGS,
    private val changed: (DownloadContinuation) -> Unit = {},
    private val cancelled: (DownloadContinuation) -> Unit = {},
    private val fragments: FragmentManager = activity.supportFragmentManager,
    private val visible: () -> Boolean = { true }) {
    val directories = DownloadDirectories(activity.applicationContext)
    private var fragment: DownloadDirectoryFragment? = null
    private var disposed = false
    private val host = object : DownloadDirectoryFragment.Host {
        override fun ready() = !disposed && visible()
        override fun directoryResolved(work: DownloadContinuation, changed: Boolean) {
            if (!disposed) { if (changed) this@DownloadDirectoryDialog.changed(work) else cancelled(work) }
        }
    }
    fun show() = begin(DownloadContinuation.Settings, false)
    fun permissionRevoked(work: DownloadContinuation) = begin(work, true)
    private fun begin(work: DownloadContinuation, explain: Boolean) {
        if (disposed || activity.isFinishing || activity.isDestroyed || fragments.isDestroyed || fragments.isStateSaved) return
        val owner = fragment ?: DownloadDirectoryFragment.obtain(fragments, tag).also { fragment = it; it.bind(host) }
        owner.begin(work, explain)
    }
    fun onResume() {
        if (!disposed) DownloadDirectoryFragment.existing(fragments, tag)?.let { fragment = it; it.bind(host) }
    }
    fun dispose() { disposed = true; fragment?.release(host); fragment = null }
    companion object { fun hasRestored(activity: FragmentActivity) = DownloadDirectoryFragment.existing(activity, DownloadDirectoryFragment.SETTINGS) != null }
}
