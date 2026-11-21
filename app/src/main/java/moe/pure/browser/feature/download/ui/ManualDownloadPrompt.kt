package moe.pure.browser.feature.download.ui

import android.app.Activity
import android.net.Uri
import moe.pure.browser.R
import moe.pure.browser.feature.download.DownloadDraft
import moe.pure.browser.feature.download.DownloadFiles
import moe.pure.browser.feature.download.DownloadNames
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.message.BrowserMessages
import java.util.Locale

/** b1.q4 form fields remain window-owned; only a value snapshot crosses recreation. */
internal class ManualDownloadPrompt(activity: Activity, private val draft: DownloadDraft, approved: (DownloadDraft) -> Unit) {
    val dialog = BrowserDialog(activity, activity.getString(R.string.ref_download_ui_new))
    private val address = dialog.field(draft.url, activity.getString(R.string.ref_download_manual_address))
    private val name = dialog.field(draft.name.orEmpty(), activity.getString(R.string.ref_download_manual_name))
    var approvedDownload = false; private set
    fun snapshot() = draft.copy(url = address.text.toString(), name = name.text.toString())

    init {
        dialog.negative().positive(activity.getString(R.string.ref_download_manual_accept)) {
            if (approvedDownload) return@positive true
            val link = address.text.toString().trim()
            val uri = Uri.parse(link)
            if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https") || uri.host.isNullOrEmpty()) {
                BrowserMessages.show(activity, R.string.ref_download_manual_unsupported); return@positive true
            }
            val filename = DownloadFiles.safeName(name.text.toString().ifBlank { DownloadNames.guess(DownloadDraft(link)) })
            val mime = DownloadNames.mime(filename)
            approvedDownload = true
            approved(draft.copy(url = link, name = filename, mime = mime)); true
        }
    }
}
