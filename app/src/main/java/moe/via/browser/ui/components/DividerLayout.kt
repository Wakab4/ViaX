package moe.via.browser.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.LinearLayout

/** Native reference toolbars draw a two-pixel translucent rule inside their own bounds. */
class DividerLayout(context: Context, private val atBottom: Boolean) : LinearLayout(context) {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x30808080 }
    init { setWillNotDraw(false) }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val top = if (atBottom) height - 2f else 0f
        canvas.drawRect(0f, top, width.toFloat(), top + 2f, line)
    }
}
