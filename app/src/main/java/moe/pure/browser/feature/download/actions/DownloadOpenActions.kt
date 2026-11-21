package moe.pure.browser.feature.download.actions

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import moe.pure.browser.R
import moe.pure.browser.feature.download.DownloadFiles
import moe.pure.browser.feature.download.DownloadTask
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.feature.download.open.FileOpenDialogFragment
import moe.pure.browser.feature.download.open.FileOpenRequest
import moe.pure.browser.feature.pdf.PdfPreview
import android.net.Uri

/** b1.l4 default open delegates installation permissions to the platform intent, as Native does. */
internal class DownloadOpenActions(private val activity: FragmentActivity, private val fragments: FragmentManager = activity.supportFragmentManager) {
    private val files = DownloadFiles(activity)
    fun open(task: DownloadTask, chooser: Boolean) {
        if (!files.exists(task)) { BrowserMessages.show(activity, R.string.ref_download_action_missing); return }
        val intent = DownloadFileIntents.view(activity, task)
        if (chooser) {
            if (!fragments.isStateSaved && fragments.findFragmentByTag(FileOpenDialogFragment.TAG) == null)
                FileOpenDialogFragment.create(FileOpenRequest(task.target, files.shareUri(task).toString(), task.mime))
                    .show(fragments, FileOpenDialogFragment.TAG)
        } else if (!launch(intent) && !launch(Intent.createChooser(intent, task.name))) {
            if (task.mime != "application/pdf" || !PdfPreview.open(activity, Uri.parse(task.target), task.name)) unavailable()
        }
    }
    private fun launch(intent: Intent) = runCatching { activity.startActivity(intent); true }.getOrDefault(false)
    private fun unavailable() = BrowserMessages.show(activity, R.string.ref_download_action_unavailable)
}
