package moe.pure.browser.feature.offline.save

import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import moe.pure.browser.core.logging.NativeLog as Log
import android.webkit.WebView
import java.io.File

/** z8.v1/a.a writes an A4 PDF directly into the archive directory, without a system print chooser. */
internal class SavedPdfWriter : AutoCloseable {
    private class Job(val adapter: PrintDocumentAdapter) {
        val cancellation = CancellationSignal()
        var output: ParcelFileDescriptor? = null
    }
    private val jobs = mutableSetOf<Job>()
    private var closed = false
    private val attributes = PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build()

    // Framework PrintDocumentAdapter.java:265/310 keeps these @hide constructors public;
    // the SDK stubs hide access. GP a.a subclasses the same callbacks directly.
    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    fun write(view: WebView, file: File) {
        if (closed) return
        val job = Job(view.createPrintDocumentAdapter("Via Document"))
        jobs.add(job)
        try {
            job.adapter.onLayout(null, attributes, job.cancellation, object : PrintDocumentAdapter.LayoutResultCallback() {
                override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) {
                    if (closed || job !in jobs) return
                    try {
                        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
                        file.createNewFile()
                        val destination = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_WRITE)
                        job.output = destination
                        job.adapter.onWrite(arrayOf(PageRange.ALL_PAGES), destination, job.cancellation,
                            object : PrintDocumentAdapter.WriteResultCallback() {
                                override fun onWriteFinished(pages: Array<out PageRange>) = finish(job)
                                override fun onWriteFailed(error: CharSequence?) = finish(job)
                                override fun onWriteCancelled() = finish(job)
                            })
                    } catch (error: Exception) { Log.warn("SavedPages", "Write PDF", error); finish(job) }
                }
                override fun onLayoutFailed(error: CharSequence?) = finish(job)
                override fun onLayoutCancelled() = finish(job)
            }, null)
        } catch (error: Exception) { finish(job); throw error }
    }

    private fun finish(job: Job) {
        if (!jobs.remove(job)) return
        runCatching { job.output?.close() }
        job.output = null
        runCatching { job.adapter.onFinish() }
    }

    override fun close() {
        if (closed) return
        closed = true
        jobs.toList().forEach { it.cancellation.cancel(); finish(it) }
    }
}
