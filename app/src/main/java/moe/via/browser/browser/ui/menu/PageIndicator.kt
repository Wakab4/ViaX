package moe.via.browser.browser.ui.menu

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

internal class PageIndicator(context: Context, private val count: Int) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var page: Int = 0
        set(value) { field = value; invalidate() }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(count * dp(2) * 2 + (count - 1).coerceAtLeast(0) * dp(4), dp(2) * 2)
    }

    override fun onDraw(canvas: Canvas) {
        if (count < 2) return
        repeat(count) { index ->
            val selected = if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) count - 1 - page else page
            paint.color = if (index == selected) context.palette.hint else 0x30808080
            canvas.drawCircle((dp(2) + index * (dp(2) * 2 + dp(4))).toFloat(), height / 2f, dp(2).toFloat(), paint)
        }
    }
}
