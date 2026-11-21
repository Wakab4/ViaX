package moe.pure.browser.feature.entry

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import moe.pure.browser.core.logging.NativeLog as Log
import android.webkit.URLUtil
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import java.util.concurrent.CancellationException

/** ua.f/c1.y copies content documents under their provider display name, otherwise keeps the URI. */
internal class EntryDocumentImporter(private val context: Context) {
    fun prepare(request: BrowserEntryRequest, cancelled: () -> Boolean, source: (InputStream?) -> Unit): BrowserEntry {
        val original = BrowserEntry(request.url, request.mimeType, request.url)
        if (!URLUtil.isContentUrl(request.url)) return original
        val uri = Uri.parse(request.url)
        if (uri.authority == null) return original
        val name = displayName(uri).takeUnless { it.isNullOrEmpty() } ?: "${UUID.randomUUID()}.html"
        val directory = context.getExternalFilesDir("opened") ?: File(context.filesDir, "opened")
        if (!directory.exists() && !directory.mkdirs()) return original
        val destination = File(directory, name)
        try {
            if (cancelled()) throw CancellationException()
            val input = context.contentResolver.openInputStream(uri) ?: return original
            source(input)
            input.use { stream ->
                if (cancelled()) throw CancellationException()
                if (!prepareDirectory(destination.parentFile) || destination.exists() && !destination.delete()) return original
                destination.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        if (cancelled()) throw CancellationException()
                        val count = stream.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.flush()
                }
            }
            return BrowserEntry(Uri.fromFile(destination).toString(), request.mimeType, request.url)
        } catch (error: IOException) {
            if (cancelled()) throw CancellationException()
            Log.warn("EntryDocuments", "Unable to copy content document", error)
            return original
        } finally { source(null) }
    }
    private fun displayName(uri: Uri): String? {
        val name = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (_: Exception) { null }
        return name ?: uri.lastPathSegment
    }
    private fun prepareDirectory(directory: File?): Boolean {
        directory ?: return false
        return if (!directory.exists()) directory.mkdirs() else directory.isDirectory || directory.delete() && directory.mkdirs()
    }
}
