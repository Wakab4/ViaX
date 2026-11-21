package moe.pure.browser.feature.offline.save

import android.app.Activity
import moe.pure.browser.core.logging.NativeLog as Log
import android.webkit.WebView
import moe.pure.browser.R
import moe.pure.browser.feature.offline.document.SavedArchiveFiles
import moe.pure.browser.ui.components.BrowserMessage
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope
import java.util.Locale
import java.util.UUID

/** P1/k0/C4/O1: form state belongs to this window; submitting resolves the then-current WebView. */
internal class SavedPageSaveController(private val activity: Activity, private val currentView: () -> WebView?,
    private val openSavedPages: () -> Unit) : AutoCloseable {
    private val preferences = SavedPagePreferences(activity.applicationContext)
    private val scope = DialogScope()
    private val messages = BrowserMessage(activity)
    private val pdfWriter = SavedPdfWriter()
    private var closed = false

    fun show(view: WebView?) {
        if (closed) return
        if (view?.url?.startsWith("file://", ignoreCase = true) == true) {
            messages.show(activity.getString(R.string.ref_saved_local_error)); return
        }
        val title = view?.title?.takeIf(String::isNotEmpty) ?: UUID.randomUUID().toString().uppercase(Locale.ROOT)
        val dialog = BrowserDialog(activity, activity.getString(R.string.ref_saved_save_title))
        val name = dialog.field(title, activity.getString(R.string.ref_saved_name_hint))
        val pdf = dialog.checkbox(activity.getString(R.string.ref_saved_as_pdf), preferences.pdf)
        scope.show(dialog.positive(activity.getString(android.R.string.ok)) {
            val raw = name.text.toString()
            if (raw.isNotEmpty()) {
                if (preferences.pdf != pdf.isChecked) preferences.pdf = pdf.isChecked
                save(SavedArchiveNames.clean(raw), pdf.isChecked)
            }
            true
        }.negative(activity.getString(android.R.string.cancel)))
    }

    private fun save(name: String, pdf: Boolean) {
        if (closed) return
        val view = currentView() ?: return
        try {
            val directory = SavedArchiveFiles.primaryDirectory(activity.applicationContext) ?: return
            val file = SavedArchiveNames.unique(directory, SavedArchiveNames.clean(name), if (pdf) ".pdf" else ".mht")
            if (pdf) pdfWriter.write(view, file)
            else {
                // Native ignores s4.b.j's false result and acknowledges initiation, rather than waiting for a callback.
                try { view.saveWebArchive(file.absolutePath) }
                catch (error: Exception) { Log.warn("SavedPages", "Start web archive", error) }
            }
            messages.show(activity.getString(R.string.ref_saved_notice), activity.getString(R.string.ref_saved_view), openSavedPages)
        } catch (error: Exception) { Log.warn("SavedPages", "Save page", error) }
    }

    override fun close() {
        if (closed) return
        closed = true; scope.dispose(); messages.close(); pdfWriter.close()
    }
}
