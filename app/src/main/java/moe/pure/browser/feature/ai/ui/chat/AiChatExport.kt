package moe.pure.browser.feature.ai.ui.chat

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import moe.pure.browser.R
import moe.pure.browser.feature.ai.session.AiConversation
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.dialog.DialogScope
import moe.pure.browser.ui.documents.NativeDocumentCreator
import moe.pure.browser.ui.tasks.NativeLifecycleJobs
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal class AiChatExport(private val owner: Fragment, private val conversation: () -> AiConversation) : AutoCloseable {
    private val jobs = NativeLifecycleJobs { owner.lifecycle }
    private val dialogs = DialogScope()
    private val creator = NativeDocumentCreator(owner, "text/plain")
    fun start(title: String?) {
        val locale = Locale.getDefault()
        val pattern = when (locale.country) { "US" -> "MMddyyyy"; "UK" -> "ddMMyyyy"; else -> "yyyyMMdd" }
        val name = title?.takeIf(String::isNotEmpty) ?: owner.getString(R.string.ref_ai_dialog_untitled)
        creator.create(name + "_" + SimpleDateFormat(pattern, locale).format(Date()) + ".txt") { uri, path -> if (uri != null) {
            val model = conversation()
            jobs.launch({
                val transcript = model.exportText()
                if (transcript == null) null else {
                    try { BufferedWriter(OutputStreamWriter(owner.context!!.contentResolver.openOutputStream(uri)!!)).use { it.write(transcript) }; true }
                    catch (error: Exception) { moe.pure.browser.core.logging.NativeLog.warn(null, error); false }
                }
            }, until = Lifecycle.Event.ON_DESTROY) { written -> if (written == true) {
                    if (path == null) model.notice.value = owner.getString(R.string.ref_ai_dialog_export_success)
                    else dialogs.show(BrowserDialog(owner.requireActivity(), owner.getString(R.string.ref_ai_dialog_notice), owner.getString(R.string.ref_ai_dialog_export_path, path)).positive(owner.getString(android.R.string.ok)))
            } }
        } }
    }
    override fun close() { jobs.close(); dialogs.dispose() }
}
