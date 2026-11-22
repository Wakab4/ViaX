package moe.via.browser.feature.ai.text.table

import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.method.MovementMethod
import android.text.style.ClickableSpan
import android.view.MotionEvent
import android.widget.TextView

/** o6.a delegates ordinary selection/link input, then maps table-cell links on ACTION_UP. */
internal class AiTableMovementMethod(private val wrapped: MovementMethod = LinkMovementMethod.getInstance()) : MovementMethod by wrapped {
    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean =
        wrapped.onTouchEvent(widget, buffer, event) || tableTouch(widget, buffer, event)
    private fun tableTouch(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return false
        val x = event.x.toInt() - widget.totalPaddingLeft + widget.scrollX
        val y = event.y.toInt() - widget.totalPaddingTop + widget.scrollY
        val layout = widget.layout!!
        val line = layout.getLineForVertical(y)
        val offset = layout.getOffsetForHorizontal(line, x.toFloat())
        val rows = buffer.getSpans(offset, offset, AiTableRowSpan::class.java)
        if (rows.isEmpty()) return false
        val row = rows[0]
        val cell = row.layoutAt(x) ?: return false
        val position = cell.getOffsetForHorizontal(cell.getLineForVertical(y - layout.getLineTop(line)), (x % row.cellWidth()).toFloat())
        val links = (cell.text as Spanned).getSpans(position, position, ClickableSpan::class.java)
        if (links.isEmpty()) return false
        links[0].onClick(widget); return true
    }
}
