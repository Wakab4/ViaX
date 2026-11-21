package moe.pure.browser.feature.home.ui.customize.palette

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** f9.a uses a 32dp corner radius, one/two dp outlines and the original inset gallery icon. */
internal object BackgroundPaletteDrawables {
    fun color(context: Context, value: Int, outline: Int = 0, width: Int = 0) = GradientDrawable().apply {
        setColor(value); cornerRadius = context.dp(32).toFloat()
        if (outline != 0) setStroke(context.dp(width), outline)
    }
    @Suppress("DEPRECATION") fun image(context: Context): Drawable {
        val ink = context.palette.icon
        val icon = context.getDrawable(R.drawable.ref_home_gallery)!!.mutate().apply { setColorFilter(ink, PorterDuff.Mode.SRC_IN) }
        return LayerDrawable(arrayOf(color(context, 0, ink, 2), icon)).apply {
            val inset = context.dp(16); setLayerInset(1, inset, inset, inset, inset)
        }
    }
}
