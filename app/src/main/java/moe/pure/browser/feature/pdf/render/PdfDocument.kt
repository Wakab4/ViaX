package moe.pure.browser.feature.pdf.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/** Owns the system renderer and descriptor. No window reference enters document or raster work. */
internal class PdfDocument(context: Context, val uri: Uri) : AutoCloseable {
    private val lock = Any()
    private val renderer: PdfRenderer
    @Volatile private var closed = false
    init {
        val descriptor = when (uri.scheme) {
            "file" -> uri.path?.let { ParcelFileDescriptor.open(File(it), ParcelFileDescriptor.MODE_READ_ONLY) }
            "content" -> context.applicationContext.contentResolver.openFileDescriptor(uri, "r")
            else -> null
        } ?: error("Cannot open PDF descriptor")
        renderer = try { PdfRenderer(descriptor) } catch (error: Exception) { descriptor.close(); throw error }
    }
    fun sizes(): List<PdfPageSize> = synchronized(lock) {
        check(!closed)
        List(renderer.pageCount) { index -> renderer.openPage(index).use { PdfPageSize(it.width, it.height) } }
    }
    fun render(index: Int, width: Int, height: Int, matrix: Matrix? = null): Bitmap = synchronized(lock) {
        check(!closed)
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        try { renderer.openPage(index).use { it.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }; bitmap }
        catch (error: Throwable) { bitmap.recycle(); throw error }
    }
    override fun close() { synchronized(lock) { if (!closed) { closed = true; renderer.close() } } }
}
