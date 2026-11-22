package moe.via.browser.feature.site.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import moe.via.browser.ui.components.NativeSeekBar
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** mark.via.common.widget.w0; DEX confirms floating point division for the highlight position. */
internal class NativeSiteSeekBar(context: Context) : NativeSeekBar(context) {
    var highlightProgress = -1
        set(value) { field = value; invalidate() }
    private val marker = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        if (max > 0 && highlightProgress in 0..max) {
            marker.color = if (highlightProgress <= progress) context.palette.accent else 0x40808080
            val fraction = (if (layoutDirection == View.LAYOUT_DIRECTION_RTL) max - highlightProgress else highlightProgress).toFloat() / max
            val x = (fraction * (width - paddingLeft - paddingRight)).toInt() + paddingLeft
            val radius = thumb?.intrinsicWidth?.takeIf { it > 0 }?.div(3f) ?: dp(2).toFloat()
            canvas.drawCircle(x.toFloat(), height / 2f, radius, marker)
        }
        super.onDraw(canvas)
    }
}
