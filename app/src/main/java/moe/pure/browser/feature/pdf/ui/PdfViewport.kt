package moe.pure.browser.feature.pdf.ui

import android.graphics.RectF
import moe.pure.browser.feature.pdf.render.PdfPageRect
import moe.pure.browser.feature.pdf.render.PdfPageSize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** PdfViewer's page geometry and clamped document transform, independent of rendering and input. */
internal class PdfViewport(private val margin: Float) {
    var width = 0
        private set
    var height = 0
        private set
    var documentHeight = 0f
        private set
    var pages: List<PdfPageRect> = emptyList()
        private set
    var zoom = 1f
        private set
    var x = 0f
        private set
    var y = 0f
        private set
    val minX get() = min(width - width * zoom, 0f)
    val minY get() = min(height - documentHeight * zoom, 0f)
    val range get() = max(height, (documentHeight * zoom).roundToInt())
    val offset get() = (-y).roundToInt().coerceIn(0, max(0, range - height))

    fun layout(sizes: List<PdfPageSize>, width: Int, height: Int, reset: Boolean) {
        val fraction = if (documentHeight == 0f) 0f else -y / zoom / documentHeight
        this.width = width; this.height = height
        var top = margin
        val available = max(1f, width - margin * 2)
        pages = sizes.mapIndexed { index, size ->
            val fit = available / max(1, size.width)
            val rect = RectF(margin, top, margin + available, top + size.height * fit)
            top = rect.bottom + margin
            PdfPageRect(index, fit, rect)
        }
        documentHeight = if (pages.isEmpty()) 0f else top
        if (reset) zoom = 1f
        move(if (reset) 0f else x, if (reset) 0f else -fraction * documentHeight * zoom)
    }

    fun clampX(value: Float) = if (zoom < 1f) max((width - width * zoom) / 2, 0f) else value.coerceIn(minX, 0f)
    fun clampY(value: Float) = value.coerceIn(minY, 0f)
    fun move(x: Float, y: Float): Boolean {
        val nextX = clampX(x); val nextY = clampY(y)
        if (this.x == nextX && this.y == nextY) return false
        this.x = nextX; this.y = nextY
        return true
    }

    fun scale(value: Float, anchorX: Float, anchorY: Float, startX: Float, startY: Float,
        startZoom: Float, midpointX: Float, midpointY: Float, snapDistance: Float) {
        zoom = value.coerceIn(.5f, 10f)
        move(midpointX - (anchorX - startX) * (zoom / startZoom),
            midpointY - (anchorY - startY) * (zoom / startZoom))
        val snappedX = clampX(0f)
        if (abs(zoom - 1f) <= .06f && abs(x - snappedX) <= snapDistance && abs(y) <= snapDistance) {
            zoom = 1f; x = snappedX; y = 0f
        }
    }

    fun visible(): IntRange {
        if (pages.isEmpty()) return IntRange.EMPTY
        val top = abs(y / zoom); val bottom = top + height / zoom
        val first = pages.indexOfFirst { it.bounds.bottom > top }.let { if (it < 0) pages.lastIndex else it }
        val last = pages.indexOfLast { it.bounds.top < bottom }.coerceAtLeast(first)
        return first..last
    }

    fun current(visible: IntRange, previous: Int): Int {
        if (visible.isEmpty()) return previous
        if (visible.first == visible.last) return visible.first
        val top = abs(y / zoom); val bottom = top + height / zoom
        var largest = 0; var chosen = previous
        for (index in visible) {
            val rect = pages[index].bounds
            val overlap = (min(rect.bottom, bottom) - max(rect.top, top)).toInt()
            if (overlap > largest) { largest = overlap; chosen = index }
        }
        return chosen
    }
}
