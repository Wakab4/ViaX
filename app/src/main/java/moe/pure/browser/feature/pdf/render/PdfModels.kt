package moe.pure.browser.feature.pdf.render

import android.graphics.Bitmap
import android.graphics.RectF

internal data class PdfPageSize(val width: Int, val height: Int)
internal data class PdfPageRect(val index: Int, val fitScale: Float, val bounds: RectF)
internal data class PdfDrawingPage(val page: PdfPageRect, val bitmap: Bitmap) {
    val drawScale get() = if (bitmap.width <= 0) 1f else page.bounds.width() / bitmap.width
}
