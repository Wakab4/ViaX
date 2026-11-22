package moe.via.browser.feature.ai.text.table

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.*
import android.text.style.ReplacementSpan
import io.noties.markwon.core.spans.TextLayoutSpan
import io.noties.markwon.utils.SpanUtils

/** o6.c measures seeded-width cell layouts before its first draw. Based on Markwon (Apache-2.0). */
internal class AiTableRowSpan(private val style: AiTableStyle, private val cells: List<AiTableCell>,
    private val header: Boolean, private val odd: Boolean) : ReplacementSpan() {
    private val layouts = ArrayList<Layout>(cells.size)
    private val textPaint = TextPaint()
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = Rect()
    private var width = if (style.initialWidth > 0) style.initialWidth else 0
    private var height = 0
    fun cellWidth() = cellWidth(layouts.size)
    private fun cellWidth(columns: Int) = (width * 1f / columns + .5f).toInt()
    fun layoutAt(x: Int): Layout? {
        val index = x / cellWidth(layouts.size)
        return if (index >= layouts.size) null else layouts[index]
    }
    private fun copyPaint(source: Paint) { if (source is TextPaint) textPaint.set(source) else textPaint.set(source) }
    @Suppress("DEPRECATION") private fun buildLayouts() {
        textPaint.isFakeBoldText = header
        val available = cellWidth(cells.size) - style.padding * 2
        layouts.clear()
        cells.forEach { cell ->
            val text = if (cell.text is Spannable) cell.text else SpannableString(cell.text)
            val alignment = when (cell.alignment) { 1 -> Layout.Alignment.ALIGN_CENTER; 2 -> Layout.Alignment.ALIGN_OPPOSITE; else -> Layout.Alignment.ALIGN_NORMAL }
            val layout = StaticLayout(text, textPaint, available, alignment, 1f, 0f, false)
            TextLayoutSpan.applyTo(text, layout); layouts.add(layout)
        }
    }
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        copyPaint(paint); buildLayouts()
        if (layouts.isNotEmpty() && fm != null) {
            height = layouts.maxOf(Layout::getHeight)
            val ascent = -(height + style.padding * 2)
            fm.ascent = ascent; fm.top = ascent; fm.descent = 0; fm.bottom = 0
        }
        return width
    }
    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val available = SpanUtils.width(canvas, text)
        if (width != available) { width = available; copyPaint(paint); buildLayouts() }
        val columns = layouts.size
        val columnWidth = cellWidth(columns)
        val rounding = columnWidth - width / columns
        fill.color = if (header) style.headerBackground else if (odd) style.oddBackground else style.evenBackground
        fill.style = Paint.Style.FILL
        if (fill.color != 0) {
            val save = canvas.save()
            try { rect.set(0, 0, width, bottom - top); canvas.translate(x, top.toFloat()); canvas.drawRect(rect, fill) }
            finally { canvas.restoreToCount(save) }
        }
        fill.set(paint); fill.color = style.borderColor; fill.style = Paint.Style.FILL
        val border = style.borderWidth
        val drawBorder = border > 0
        val rowHeight = bottom - top
        val offset = (rowHeight - height) / 4
        var first = false
        if (drawBorder) {
            val spanned = text as Spanned
            val blocks = spanned.getSpans(start, end, AiTableBlockSpan::class.java)
            first = blocks.isNotEmpty() && spanned.getSpanStart(blocks[0]) == start
            if (first) { rect.set(x.toInt(), top, width, top + border); canvas.drawRect(rect, fill) }
            rect.set(x.toInt(), bottom - border, width, bottom); canvas.drawRect(rect, fill)
        }
        val half = border / 2
        val borderTop = if (first) border else 0
        val borderBottom = rowHeight - border
        layouts.forEachIndexed { index, layout ->
            val save = canvas.save()
            try {
                canvas.translate(x + index * columnWidth, top.toFloat())
                if (drawBorder) {
                    if (index == 0) rect.set(0, borderTop, border, borderBottom) else rect.set(-half, borderTop, half, borderBottom)
                    canvas.drawRect(rect, fill)
                    if (index == columns - 1) { rect.set(columnWidth - border - rounding, borderTop, columnWidth - rounding, borderBottom); canvas.drawRect(rect, fill) }
                }
                canvas.translate(style.padding.toFloat(), (style.padding + offset).toFloat()); layout.draw(canvas)
            } finally { canvas.restoreToCount(save) }
        }
    }
}
