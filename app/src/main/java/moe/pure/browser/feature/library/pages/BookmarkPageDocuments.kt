package moe.pure.browser.feature.library.pages

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import moe.pure.browser.R
import moe.pure.browser.feature.library.BookmarkHtmlCodec
import moe.pure.browser.feature.library.LibraryRepository
import moe.pure.browser.ui.documents.NativeDocumentPicker
import moe.pure.browser.ui.message.BrowserMessages
import moe.pure.browser.ui.tasks.NativeViewJobs
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The actual bookmark child registers both document contracts before it starts. */
internal class BookmarkPageDocuments(private val owner: Fragment, private val repository: () -> LibraryRepository, private val changed: () -> Unit) : AutoCloseable {
    private val picker = NativeDocumentPicker(owner)
    private var jobs: NativeViewJobs? = null
    private val exporter = owner.registerForActivityResult(ActivityResultContracts.CreateDocument("text/html")) { uri -> if (uri != null) write(uri) }
    fun bind() { jobs = NativeViewJobs() }
    fun import() {
        picker.open(arrayOf("text/html")) { uri -> if (uri != null) {
            val app = owner.requireContext().applicationContext
            jobs?.launch({
                val input = requireNotNull(app.contentResolver.openInputStream(uri))
                input.reader(Charset.defaultCharset()).use { BookmarkHtmlCodec.import(it, repository()) }
            }) { count ->
                val text = if (count > 0) owner.getString(R.string.ref_bookmark_document_imported, count) else owner.getString(R.string.ref_bookmark_document_import_failed)
                BrowserMessages.show(owner.requireActivity(), text); if (count > 0) changed()
            }
        } }
    }
    fun export() {
        val locale = Locale.getDefault(); val pattern = when (locale.country) { "US" -> "MMddyyyy"; "UK" -> "ddMMyyyy"; else -> "yyyyMMdd" }
        exporter.launch("Via_" + owner.getString(R.string.ref_bookmark_page_title) + "_" + SimpleDateFormat(pattern, locale).format(Date()) + ".html")
    }
    private fun write(uri: Uri) {
        val app = owner.requireContext().applicationContext
        jobs?.launch({
            val output = requireNotNull(app.contentResolver.openOutputStream(uri))
            output.writer(Charsets.UTF_8).use { BookmarkHtmlCodec.export(it, repository()) }
        }) { count -> BrowserMessages.show(owner.requireActivity(), owner.getString(when {
            count > 0 -> R.string.ref_bookmark_document_exported; count == 0 -> R.string.ref_bookmark_document_export_empty; else -> R.string.ref_bookmark_document_failure
        })) }
    }
    override fun close() { jobs?.close(); jobs = null }
}
