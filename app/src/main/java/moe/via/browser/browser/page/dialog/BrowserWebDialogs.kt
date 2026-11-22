package moe.via.browser.browser.page.dialog

import android.app.Activity
import android.os.Message
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.WebView
import moe.via.browser.R
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.dialog.DialogScope

enum class JavaScriptDialogKind { ALERT, CONFIRM, PROMPT, BEFORE_UNLOAD }

/** Resolves each suspended WebView callback once; dialogs never outlive their owning document. */
class BrowserWebDialogs(private val activity: Activity) {
    private class Pending(val view: WebView, val cancel: () -> Unit) {
        var dialog: BrowserDialog? = null
        var answered = false
        fun answer(action: () -> Unit) { if (!answered) { answered = true; action() } }
        fun dismiss() { answer(cancel); dialog?.dismiss() }
    }
    private val dialogs = DialogScope()
    private val pending = mutableSetOf<Pending>()
    private var disposed = false

    fun show(view: WebView, kind: JavaScriptDialogKind, url: String, message: String?, defaultValue: String?, result: JsResult): Boolean {
        val visible = !disposed && !activity.isFinishing && !activity.isDestroyed && view.isShown
        if (!visible) {
            if (kind == JavaScriptDialogKind.BEFORE_UNLOAD) result.confirm() else result.cancel()
            return true
        }
        if (kind == JavaScriptDialogKind.PROMPT && message?.startsWith("BdboxApp:{\"obj\":\"") == true) {
            (result as JsPromptResult).confirm(""); return true
        }
        val site = site(url)
        val isConfirm = kind == JavaScriptDialogKind.CONFIRM
        val tracked = kind == JavaScriptDialogKind.ALERT || isConfirm
        val status = if (tracked) JavaScriptMessageHistory.status(site.ifEmpty { "default" }, isConfirm) else 0
        if (status == 2) { result.cancel(); return true }
        val title = if (kind == JavaScriptDialogKind.BEFORE_UNLOAD) activity.getString(R.string.web_dialog_leave_title)
            else if (site.isEmpty()) activity.getString(R.string.web_dialog_title) else activity.getString(R.string.web_dialog_site_title, site)
        val dialog = BrowserDialog(activity, title, message?.takeIf { it.isNotEmpty() })
        val item = Pending(view, result::cancel)
        val muted = if (status == 1) dialog.checkbox(activity.getString(R.string.web_dialog_mute_minute)) else null
        val input = if (kind == JavaScriptDialogKind.PROMPT) dialog.field(defaultValue.orEmpty(), defaultValue.orEmpty()) else null
        fun answer(accepted: Boolean) = item.answer {
            if (accepted) {
                if (result is JsPromptResult) result.confirm(input?.text?.toString().orEmpty()) else result.confirm()
            } else result.cancel()
            if (tracked) JavaScriptMessageHistory.answered(site.ifEmpty { "default" }, isConfirm, status == 1 && muted?.isChecked == true)
        }
        dialog.positive(activity.getString(if (kind == JavaScriptDialogKind.BEFORE_UNLOAD) R.string.web_dialog_leave else android.R.string.ok)) { answer(true); true }
        if (kind != JavaScriptDialogKind.ALERT) {
            dialog.negative(activity.getString(if (kind == JavaScriptDialogKind.BEFORE_UNLOAD) R.string.web_dialog_stay else android.R.string.cancel)) { answer(false); true }
        }
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnCancelListener { item.answer(result::cancel) }
        item.dialog = dialog
        pending += item
        dialogs.show(dialog) { item.answer(result::cancel); pending.remove(item) }
        return true
    }

    fun resubmit(view: WebView, doNotResend: Message, resend: Message) {
        if (disposed || !view.isShown || activity.isFinishing || activity.isDestroyed) { doNotResend.sendToTarget(); return }
        val item = Pending(view, doNotResend::sendToTarget)
        val dialog = BrowserDialog(activity, activity.getString(R.string.web_dialog_form_title), activity.getString(R.string.web_dialog_form_message))
            .positive(activity.getString(android.R.string.ok)) { item.answer(resend::sendToTarget); true }
            .negative(activity.getString(android.R.string.cancel)) { item.answer(doNotResend::sendToTarget); true }
        dialog.setCancelable(true); dialog.setCanceledOnTouchOutside(false)
        dialog.setOnCancelListener { item.answer(doNotResend::sendToTarget) }
        item.dialog = dialog
        pending += item
        dialogs.show(dialog) { item.answer(doNotResend::sendToTarget); pending.remove(item) }
    }
    fun navigation(view: WebView) { pending.filter { it.view === view }.toList().forEach(Pending::dismiss) }
    fun selected(view: WebView?) { pending.filter { it.view !== view }.toList().forEach(Pending::dismiss) }
    fun dispose() { disposed = true; pending.toList().forEach(Pending::dismiss); pending.clear(); dialogs.dispose() }

    /** Native i6.i0.f uses the authority substring up to its first colon, including its IPv6 quirk. */
    private fun site(url: String): String {
        val marker = url.indexOf("://")
        if (marker < 0) return ""
        return url.substring(marker + 3).substringBefore('/').substringBefore(':')
    }
}
