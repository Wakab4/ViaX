package moe.pure.browser.feature.offline.document

import android.net.Uri
import moe.pure.browser.R
import moe.pure.browser.ui.components.BrowserMessage
import moe.pure.browser.ui.theme.ThemedActivity

/** Context action 18 deletes the exact archive, then runs the Native current-page dirty refresh. */
class SavedDocumentActions(private val activity: ThemedActivity, private val refreshCurrent: () -> Unit) {
    private val app = activity.applicationContext
    private val message = BrowserMessage(activity)
    private var disposed = false

    fun delete(url: String) {
        if (disposed) return
        val path = if (url.startsWith("file://") && url.lastIndexOf('/') >= 7)
            url.substring(7, url.lastIndexOf('/') + 1) + Uri.decode(url.substring(url.lastIndexOf('/') + 1)) else url
        // Z5(18) completes c1.g before y1; selecting another tab cannot move between these steps.
        val deleted = runCatching { SavedArchiveFiles.delete(app, url) }.getOrDefault(false)
        if (deleted) refreshCurrent()
        else message.show(activity.getString(R.string.ref_saved_delete_failed_prefix) + path)
    }
    fun dispose() { disposed = true; message.close() }
}
