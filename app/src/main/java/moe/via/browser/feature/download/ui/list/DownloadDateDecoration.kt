package moe.via.browser.feature.download.ui.list

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.feature.download.model.DownloadTask
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** b1.V1 + v8/c6: decoration positions refer to actual download records, never synthetic rows. */
internal class DownloadDateDecoration(private val context: Context) : RecyclerView.ItemDecoration() {
    private data class Section(val first: Int, val label: String)
    private var sections = emptyList<Section>()
    private var count = 0
    private val height = context.dp(32)
    private val inset = context.dp(18)
    private val topInset = context.dp(4)
    private val slide = context.dp(6)
    private val rtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = context.resources.displayMetrics.scaledDensity * 11f
        if (rtl) textAlign = Paint.Align.RIGHT
    }

    fun submit(tasks: List<DownloadTask>) {
        count = tasks.size
        val calendar = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
        val yearLimit = -calendar.get(Calendar.DAY_OF_YEAR)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        var day = calendar.timeInMillis / 1000
        var relative = 1
        val current = SimpleDateFormat(context.getString(R.string.ref_library_history_current_year), Locale.getDefault())
        val other = SimpleDateFormat(context.getString(R.string.ref_library_history_other_year), Locale.getDefault())
        sections = buildList {
            tasks.forEachIndexed { index, task ->
                // The download record already stores milliseconds; g5.c.b is seconds.
                val stamp = task.created / 1000
                if (stamp < day) {
                    while (day > stamp) { day -= 86400; relative-- }
                    val label = when (relative) {
                        0 -> context.getString(R.string.ref_library_history_today)
                        -1 -> context.getString(R.string.ref_library_history_yesterday)
                        else -> (if (relative > yearLimit) current else other).format(Date(day * 1000))
                    }
                    add(Section(index, label))
                }
            }
        }
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.top = if (sections.any { it.first == parent.getChildAdapterPosition(view) }) height else 0
    }

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        paint.typeface = parent.getChildAt(0)?.findViewById<TextView>(R.id.download_name)?.typeface
        for (index in 0 until parent.childCount) {
            val child = parent.getChildAt(index)
            val position = parent.getChildAdapterPosition(child)
            val group = sections.indexOfLast { it.first <= position }
            if (position !in 0 until count || group < 0) continue
            val next = sections.getOrNull(group + 1)?.first ?: count
            val translation = (child.translationY + .5f).toInt()
            if (index == 0) {
                val top = if (position == next - 1) minOf(child.bottom - height, parent.paddingTop) else parent.paddingTop
                val fraction = if (child.top >= 0) 1f - child.top.toFloat() / height else 1f
                draw(canvas, sections[group].label, parent.paddingLeft, top,
                    parent.width - parent.paddingRight, top + height, translation, fraction)
            } else if (position == sections[group].first) draw(canvas, sections[group].label, parent.paddingLeft,
                child.top - height, parent.width - parent.paddingRight, child.top, translation, 0f)
        }
    }

    private fun draw(canvas: Canvas, text: String, left: Int, top: Int, right: Int, bottom: Int, translation: Int, fraction: Float) {
        val y = top + translation
        val edge = bottom + translation
        val color = context.palette.canvas
        paint.color = Color.argb((fraction * 255).roundToInt().coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
        val offset = fraction * slide
        val width = paint.measureText(text)
        val start = if (rtl) right - inset / 2f - inset - width - offset else left + inset / 2f + offset
        val end = if (rtl) right - inset / 2f - offset else left + width + inset + inset / 2f + offset
        canvas.drawRoundRect(start, (y + topInset).toFloat(), end, edge.toFloat(), inset.toFloat(), inset.toFloat(), paint)
        paint.color = context.palette.secondary
        val baseline = (edge - y) / 2f - (paint.descent() + paint.ascent()) / 2f + y + topInset
        canvas.drawText(text, if (rtl) right - inset - offset else left + inset + offset, baseline, paint)
    }
}
