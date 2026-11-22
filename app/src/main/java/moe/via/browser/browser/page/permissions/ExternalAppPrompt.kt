package moe.via.browser.browser.page.permissions

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import moe.via.browser.R
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope
import moe.via.browser.ui.message.BrowserMessages
import moe.via.browser.ui.message.MessageContent
import moe.via.browser.ui.message.MessageOwner

/** s6.J9's f6.h row; only its action's long press opens s6.b4's URL details dialog. */
internal class ExternalAppPrompt(private val activity: Activity) {
    private val notice = MessageOwner(activity)
    private val details = DialogScope()
    private var disposed = false

    fun show(targetUrl: String, applicationLabel: String, open: () -> Unit) {
        if (disposed) return
        val text = if (applicationLabel.isEmpty()) activity.getString(R.string.ref_external_app_question)
            else activity.getString(R.string.ref_external_app_question_name, applicationLabel)
        notice.show(MessageContent(text, activity.getString(android.R.string.ok),
            clicked = open,
            longClicked = {
                if (disposed || targetUrl.isEmpty() || activity.isFinishing || activity.isDestroyed) false
                else { showDetails(targetUrl, open); true }
            }))
        // No remember checkbox or site write: timeout merely leaves the request unopened.
        // Do not complete a negative callback at fade start; Native keeps that row's action clickable.
    }

    private fun showDetails(targetUrl: String, open: () -> Unit) {
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_external_app_details_title), targetUrl)
            .positive(activity.getString(R.string.ref_external_app_open)) { open(); true }
            .negative(activity.getString(android.R.string.cancel))
            .neutral(activity.getString(android.R.string.copy)) {
                val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText(null, targetUrl))
                    BrowserMessages.show(activity, R.string.ref_external_app_copied)
                }
                true
            }
        details.show(dialog)
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        notice.close(false)
        details.dispose()
    }
}
