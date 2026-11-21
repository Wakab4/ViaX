package moe.pure.browser.feature.library.ui.recycler

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.library.LibraryEntry
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** db.n + c6.b/v8: date sections are 32 dp decorations, so adapter positions remain record positions. */
internal class LibraryDateDecoration(private val context: Context, private val milliseconds: Boolean = false) : RecyclerView.ItemDecoration() {
    private data class Section(val first: Int, val label: String)
    private var sections: List<Section> = emptyList()
    private var count = 0
    private val height = context.dp(32)
    private val inset = context.dp(18)
    private val topInset = context.dp(4)
    private val slide = context.dp(12)
    private val rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = context.resources.displayMetrics.scaledDensity * 11f
        if (rtl) textAlign = Paint.Align.RIGHT
    }
    fun submit(entries: List<LibraryEntry>) {
        count = entries.size
        val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
        val currentYearLimit = -calendar.get(Calendar.DAY_OF_YEAR)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val unit = if (milliseconds) 1L else 1000L
        val step = 86400000L / unit
        var day = calendar.timeInMillis / unit
        var relative = 1
        val current = SimpleDateFormat(context.getString(R.string.ref_library_history_current_year), Locale.getDefault())
        val other = SimpleDateFormat(context.getString(R.string.ref_library_history_other_year), Locale.getDefault())
        sections = buildList {
            entries.forEachIndexed { index, entry ->
                val stamp = entry.updatedAt
                if (stamp < day) {
                    while (day > stamp) { day -= step; relative-- }
                    val label = when (relative) {
                        0 -> context.getString(R.string.ref_library_history_today)
                        -1 -> context.getString(R.string.ref_library_history_yesterday)
                        else -> (if (relative > currentYearLimit) current else other).format(Date(day * unit))
                    }
                    add(Section(index, label))
                }
            }
        }
    }
    private fun section(position: Int) = sections.indexOfLast { it.first <= position }
    override fun getItemOffsets(out: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view)
        out.top = if (sections.any { it.first == position }) height else 0
    }
    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        paint.typeface = (parent.getChildAt(0) as? LibraryItemView)?.title?.typeface
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val position = parent.getChildAdapterPosition(child)
            val group = section(position)
            if (position !in 0 until count || group < 0) continue
            val next = sections.getOrNull(group + 1)?.first ?: count
            val translation = (child.translationY + .5f).toInt()
            if (index == 0) {
                val top = if (position == next - 1) minOf(child.bottom - height, parent.paddingTop) else parent.paddingTop
                val fraction = if (child.top >= 0) 1f - child.top.toFloat() / height else 1f
                drawHeader(canvas, sections[group].label, parent.paddingLeft, top, parent.width - parent.paddingRight, top + height, translation, fraction)
            } else if (position == sections[group].first) {
                drawHeader(canvas, sections[group].label, parent.paddingLeft, child.top - height, parent.width - parent.paddingRight, child.top, translation, 0f)
            }
        }
    }
    private fun drawHeader(canvas: Canvas, text: String, left: Int, top: Int, right: Int, bottom: Int, translation: Int, fraction: Float) {
        val y = top + translation
        val edge = bottom + translation
        val color = context.palette.canvas
        paint.color = Color.argb((fraction * 255).roundToInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
        val offset = fraction * slide
        val width = paint.measureText(text)
        val radius = inset.toFloat()
        val start = if (rtl) right - inset / 2f - inset - width - offset else left + inset / 2f + offset
        val end = if (rtl) right - inset / 2f - offset else left + width + inset + inset / 2f + offset
        canvas.drawRoundRect(start, (y + topInset).toFloat(), end, edge.toFloat(), radius, radius, paint)
        paint.color = context.palette.secondary
        val baseline = (edge - y) / 2f - (paint.descent() + paint.ascent()) / 2f + y + topInset
        canvas.drawText(text, if (rtl) right - inset - offset else left + inset + offset, baseline, paint)
    }
}
