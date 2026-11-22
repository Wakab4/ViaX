package moe.via.browser.feature.home.ui.favorites

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import moe.via.browser.R
import moe.via.browser.ui.theme.skin.NativeSkinIcons
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** w3.p uses the original dashed add surface, including the explicit ic_add skin key. */
internal object HomeIconPlaceholder {
    fun create(context: Context, radius: Int): Drawable {
        val background = GradientDrawable().apply {
            setColor(549503168)
            cornerRadius = radius.toFloat()
            setStroke(context.dp(1), 1073741824, context.dp(4).toFloat(), context.dp(4).toFloat())
        }
        val plus = NativeSkinIcons(context).resolve(R.drawable.ref_home_icon_add, "ic_add") ?: context.getDrawable(R.drawable.ref_home_icon_add)
        plus?.setColorFilter(context.palette.hint, PorterDuff.Mode.SRC_IN)
        return LayerDrawable(arrayOf(background, plus)).apply {
            val inset = context.dp(12)
            setLayerInset(1, inset, inset, inset, inset)
        }
    }
}
