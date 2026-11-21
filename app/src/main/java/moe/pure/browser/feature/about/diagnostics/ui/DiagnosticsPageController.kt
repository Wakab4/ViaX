package moe.pure.browser.feature.about.diagnostics.ui

import moe.pure.browser.R
import moe.pure.browser.feature.about.diagnostics.DiagnosticsInfo
import moe.pure.browser.feature.about.diagnostics.DiagnosticsLogCursor
import moe.pure.browser.feature.about.diagnostics.DiagnosticsRepository
import moe.pure.browser.ui.dialog.BrowserDialog
import moe.pure.browser.ui.tasks.NativeViewJobs
import moe.pure.browser.ui.theme.ThemedActivity
import java.io.File

internal class DiagnosticsPageController(private val activity: ThemedActivity, applyInsets: Boolean, back: () -> Unit) {
    private val repository = DiagnosticsRepository()
    private val jobs = NativeViewJobs()
    private val sharing = DiagnosticsSharing(activity, jobs)
    private val cursor = DiagnosticsLogCursor()
    private var files: List<File> = emptyList()
    private var reading = false
    private val page = DiagnosticsPageView(activity, applyInsets, back, ::confirmDelete, { sharing.share(files) }, ::next)
    val view get() = page.root

    fun load() {
        files = repository.files()
        page.actions(files.isNotEmpty())
        page.replace(DiagnosticsInfo.capture(activity) + "\n\n")
        cursor.file = 0; cursor.skipped = 0; reading = false
        next()
    }

    private fun next() {
        if (reading || cursor.file < 0 || cursor.file >= files.size) return
        reading = true
        jobs.launch({ repository.read(files, cursor) }) { text -> reading = false; page.append(text) }
    }

    private fun confirmDelete() {
        val selected = files
        if (selected.isEmpty()) return
        activity.dialogScope.show(BrowserDialog(activity, activity.getString(R.string.ref_diagnostics_delete),
            activity.getString(R.string.ref_diagnostics_delete_message, "logs"))
            .positive(activity.getString(R.string.ref_diagnostics_delete)) {
                jobs.launch({ repository.delete(selected) }, failed = { it.printStackTrace() }) { count -> if (count > 0) load() }
                true
            }.negative(activity.getString(android.R.string.cancel)))
    }

    fun close() { jobs.close(); sharing.close(); page.close() }
}
