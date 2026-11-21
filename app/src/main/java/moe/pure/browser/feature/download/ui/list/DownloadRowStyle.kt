package moe.pure.browser.feature.download.ui.list

import android.content.Context
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** drawable p/o and DownloadTaskViewDelegate.d.Z; dimensions belong to layout/z. */
internal object DownloadRowStyle {
    fun pressed(context: Context, round: Boolean = false) = StateListDrawable().apply {
        val shape = GradientDrawable().apply {
            setColor(0x30808080)
            if (round) cornerRadius = context.dp(18).toFloat()
        }
        addState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed), shape)
        addState(intArrayOf(), ColorDrawable(android.graphics.Color.TRANSPARENT))
    }

    fun progress(context: Context): LayerDrawable {
        val radius = context.dp(2).toFloat()
        val background = GradientDrawable().apply { setColor(0x30808080); cornerRadius = radius }
        val fill = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(context.palette.accent,
            if (context.palette.night) 0xFF48598B.toInt() else 0xFF5B73B5.toInt())).apply { cornerRadius = radius }
        return LayerDrawable(arrayOf(background, ClipDrawable(fill, Gravity.LEFT, ClipDrawable.HORIZONTAL))).apply {
            setId(0, android.R.id.background); setId(1, android.R.id.progress)
        }
    }
}
