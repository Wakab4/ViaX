package moe.via.browser.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import moe.via.browser.ui.theme.palette

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
fun View.dp(value: Int): Int = context.dp(value)
fun Context.surface(color: Int = palette.panel, radius: Int = 0) = GradientDrawable().apply {
    setColor(color)
    cornerRadius = dp(radius).toFloat()
}
fun Context.touchFeedback(radius: Int = 0) = RippleDrawable(
    ColorStateList.valueOf(if (palette.night) 0x18FFFFFF else 0x18000000),
    null,
    surface(radius = radius)
)

