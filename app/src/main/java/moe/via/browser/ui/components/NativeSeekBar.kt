package moe.via.browser.ui.components

import android.content.Context
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.ContextThemeWrapper
import android.widget.SeekBar
import moe.via.browser.R
import moe.via.browser.ui.theme.palette

/** r3.g + style/s are shared by the plain reader slider and the highlighted site slider. */
internal open class NativeSeekBar(context: Context) : SeekBar(ContextThemeWrapper(context, R.style.Widget_Via_NativeSeekBar)) {
    init {
        fun track(color: Int) = GradientDrawable().apply { cornerRadius = dp(5).toFloat(); setColor(color); setSize(0, dp(2)) }
        progressDrawable = LayerDrawable(arrayOf(track(0x40808080), ClipDrawable(track(context.palette.accent), 3, 1))).apply {
            setId(0, android.R.id.background); setId(1, android.R.id.progress)
        }
        thumb = GradientDrawable().apply { cornerRadius = dp(6).toFloat(); setColor(context.palette.accent); setSize(dp(12), dp(12)) }
    }
}
