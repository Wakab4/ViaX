package moe.pure.browser.feature.passwords.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import moe.pure.browser.R
import moe.pure.browser.core.files.NativeStreams
import moe.pure.browser.core.logging.NativeLog
import moe.pure.browser.feature.passwords.auth.DeviceAuthentication
import moe.pure.browser.feature.passwords.data.PasswordRepository
import moe.pure.browser.feature.passwords.storage.PasswordCsvDownloads
import moe.pure.browser.ui.documents.NativeDocumentPicker
import moe.pure.browser.ui.tasks.NativeViewJobs
import moe.pure.browser.ui.theme.ThemedActivity
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** p8 callbacks belong to the Fragment; export lives to destroy and import only to its view. */
internal class PasswordCsvDocuments(private val owner: Fragment, private val repository: () -> PasswordRepository,
    private val authentication: () -> DeviceAuthentication, message: (String) -> Unit) {
    private val activity get() = owner.requireActivity() as ThemedActivity
    private val downloads by lazy { PasswordCsvDownloads(activity) }
    private val feedbackDelegate = lazy { PasswordCsvFeedback(activity, message) }
    private val feedback by feedbackDelegate
    private val exportJobs = NativeViewJobs()
    private var importJobs: NativeViewJobs? = null
    private var exportResult: ((Uri?, String?) -> Unit)? = null
    private var permissionAction: (() -> Unit)? = null
    private val permission = if (Build.VERSION.SDK_INT == 28) owner.registerForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        if (allowed) { permissionAction?.invoke(); permissionAction = null } else feedback.permissionDenied()
    } else null
    private val output = owner.registerForActivityResult(object : ActivityResultContract<String, Uri?>() {
        override fun createIntent(context: Context, input: String) = Intent(Intent.ACTION_CREATE_DOCUMENT).setType("text/csv").putExtra(Intent.EXTRA_TITLE, input)
        override fun parseResult(resultCode: Int, intent: Intent?) = if (resultCode == Activity.RESULT_OK) intent?.data else null
    }) { uri -> delivered(uri, null) }
    private val input = NativeDocumentPicker(owner)
    fun bindView() { importJobs = NativeViewJobs() }
    fun releaseView() { importJobs?.close(); importJobs = null }
    fun importPasswords() = input.open(arrayOf("text/*")) { uri -> if (uri != null) importJobs?.launch({
        val reader = BufferedReader(InputStreamReader(activity.contentResolver.openInputStream(uri)))
        try { repository().importCsv(reader) } finally { NativeStreams.close(reader) }
    }, delivered = feedback::imported) }
    fun export() {
        if (repository().count() == 0) { feedback.empty(); return }
        authentication().authenticate(activity.getString(R.string.ref_password_csv_export), activity.getString(R.string.ref_password_csv_export_auth_description), {
            exportResult = { uri, path -> if (uri != null) exportJobs.launch({
                val writer = BufferedWriter(OutputStreamWriter(activity.contentResolver.openOutputStream(uri)))
                try { repository().exportCsv(writer) } finally { NativeStreams.close(writer) }
            }) { feedback.exported(path) } }
            val name = filename()
            try { output.launch(name) } catch (error: ActivityNotFoundException) {
                NativeLog.warn(null, error)
                permissionAction = { val created = downloads.create(name); delivered(created.uri, created.displayPath) }
                val gate = permission
                if (gate == null) permissionAction!!.invoke() else gate.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        })
    }
    private fun delivered(uri: Uri?, path: String?) {
        val callback = exportResult ?: return
        callback(uri, path); exportResult = null
    }
    private fun filename(): String {
        val locale = Locale.getDefault()
        val pattern = when (locale.country) { "US" -> "MMddyyyy"; "UK" -> "ddMMyyyy"; else -> "yyyyMMdd" }
        return activity.getString(R.string.ref_password_page_app_name) + "_" + activity.getString(R.string.ref_password_csv_saved) + "_" + SimpleDateFormat(pattern, locale).format(Date()) + ".csv"
    }
    fun dispose() { releaseView(); exportJobs.close(); if (feedbackDelegate.isInitialized()) feedback.dispose() }
}
