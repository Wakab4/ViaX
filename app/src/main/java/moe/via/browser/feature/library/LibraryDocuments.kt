package moe.via.browser.feature.library

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import moe.via.browser.R
import moe.via.browser.ui.components.BrowserMessage
import java.lang.ref.WeakReference
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor
import moe.via.browser.feature.library.data.LibraryRepository

/** Manual document UI; the actual UUID graph/seconds are shared with archive HTML. */
class LibraryDocuments(private val activity: Activity, @Suppress("UNUSED_PARAMETER") repository: LibraryRepository,
    private val worker: Executor, private val onChanged: () -> Unit,
    private val launchDocument: (Intent, Int) -> Unit) : AutoCloseable {
    private val app = activity.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val messages = BrowserMessage(activity)
    private var closed = false

    @Suppress("UNUSED_PARAMETER")
    fun import(folder: String) {
        if (closed) return
        launch(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/html"))
            putExtra("android.provider.extra.INITIAL_URI",
                Uri.parse("content://com.android.externalstorage.documents/document/primary:Download"))
        }, IMPORT)
    }

    fun export() {
        if (closed) return
        val locale = Locale.getDefault()
        val pattern = when (locale.country) { "US" -> "MMddyyyy"; "UK" -> "ddMMyyyy"; else -> "yyyyMMdd" }
        val stamp = SimpleDateFormat(pattern, locale).format(Date())
        val name = activity.getString(R.string.app_name) + "_" + activity.getString(R.string.ref_bookmark_page_title) + "_" + stamp + ".html"
        launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "text/html"
            putExtra(Intent.EXTRA_TITLE, name)
        }, EXPORT)
    }

    private fun launch(intent: Intent, request: Int) {
        try {
            @Suppress("DEPRECATION")
            launchDocument(intent, request)
        } catch (_: ActivityNotFoundException) {
            messages.show(activity.getString(R.string.ref_bookmark_document_failure))
        }
    }

    fun onResult(request: Int, result: Int, uri: Uri?): Boolean {
        if (request != IMPORT && request != EXPORT) return false
        if (closed || result != Activity.RESULT_OK || uri == null) return true
        val application = app
        val owner = WeakReference(this)
        val dispatcher = main
        worker.execute {
            val outcome = runCatching {
                // This job owns its database handle; closing the old list window cannot close it.
                LibraryRepository(application).use { repository ->
                    if (request == IMPORT) {
                        val input = application.contentResolver.openInputStream(uri) ?: return@use 0
                        input.reader(Charset.defaultCharset()).use { BookmarkHtmlCodec.import(it, repository) }
                    } else {
                        val output = application.contentResolver.openOutputStream(uri) ?: return@use -1
                        output.writer(Charsets.UTF_8).use { BookmarkHtmlCodec.export(it, repository) }
                    }
                }
            }
            dispatcher.post {
                val window = owner.get()?.takeUnless { it.closed || it.activity.isFinishing || it.activity.isDestroyed } ?: return@post
                val count = outcome.getOrDefault(if (request == IMPORT) 0 else -1)
                val text = if (request == IMPORT) {
                    if (count > 0) window.activity.getString(R.string.ref_bookmark_document_imported, count)
                    else window.activity.getString(R.string.ref_bookmark_document_import_failed)
                } else when {
                    count > 0 -> window.activity.getString(R.string.ref_bookmark_document_exported)
                    count == 0 -> window.activity.getString(R.string.ref_bookmark_document_export_empty)
                    else -> window.activity.getString(R.string.ref_bookmark_document_failure)
                }
                window.messages.show(text)
                if (request == IMPORT && count > 0) window.onChanged()
            }
        }
        return true
    }

    override fun close() { if (!closed) { closed = true; messages.close() } }

    companion object { private const val IMPORT = 301; private const val EXPORT = 302 }
}
