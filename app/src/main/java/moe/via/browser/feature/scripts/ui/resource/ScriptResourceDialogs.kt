package moe.via.browser.feature.scripts.ui.resource

import android.content.ClipData
import android.content.ClipboardManager
import androidx.lifecycle.Lifecycle
import moe.via.browser.R
import moe.via.browser.feature.download.model.DownloadDraft
import moe.via.browser.feature.download.DownloadNames
import moe.via.browser.feature.scripts.model.ScriptDetailsResource
import moe.via.browser.feature.scripts.model.ScriptResourceInfo
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.tasks.NativeLifecycleJobs
import moe.via.browser.ui.theme.ThemedActivity
import java.io.File

/** ta.u checks the selected row's current file and binds download delivery to its View scope. */
internal class ScriptResourceDialogs(private val activity: ThemedActivity, lifecycle: () -> Lifecycle,
    private val fetch: (String) -> Boolean, private val changed: (Int) -> Unit,
    private val downloaded: (String) -> Unit, private val openFile: (String) -> Unit) : AutoCloseable {
    private val scope = DialogScope()
    private val jobs = NativeLifecycleJobs(lifecycle)

    fun show(value: ScriptDetailsResource, position: Int) {
        val path = value.path
        var info: ScriptResourceInfo = ScriptResourceInfo.Inline(value.url)
        if (!path.isNullOrEmpty()) {
            val file = File(path)
            if (!file.exists()) {
                if (value.cached) { value.cached = false; changed(position) }
                scope.show(BrowserDialog(activity, activity.getString(R.string.ref_script_resource_title), activity.getString(R.string.ref_script_resource_missing))
                    .positive(activity.getString(R.string.ref_script_resource_download)) { download(value.url); true }
                    .negative(activity.getString(android.R.string.cancel))
                    .neutral(activity.getString(R.string.ref_script_resource_copy)) { copy(value.url); true })
                return
            }
            info = ScriptResourceInfo.Cached(value.url, file, file.length(), file.lastModified())
        }
        if (!value.cached) { value.cached = true; changed(position) }
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_script_resource_title), ScriptResourcePresentation.description(activity, info))
            .positive(activity.getString(android.R.string.ok))
        if (info is ScriptResourceInfo.Cached) dialog.neutral(activity.getString(R.string.ref_script_resource_view)) { openFile(path!!); true }
        scope.show(dialog)
    }

    private fun copy(url: String) {
        activity.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, url))
        BrowserMessages.show(activity, R.string.ref_script_resource_copied)
    }

    private fun download(url: String) {
        BrowserMessages.show(activity, R.string.ref_script_resource_downloading)
        jobs.launch({ fetch(url) }) { successful ->
            if (successful) downloaded(url)
            else BrowserMessages.show(activity, activity.getString(R.string.ref_script_resource_download_failed, DownloadNames.guess(DownloadDraft(url))))
        }
    }

    override fun close() { jobs.close(); scope.dispose() }
}
