package moe.pure.browser.feature.home.ui.favorites

import android.content.Context
import android.content.res.Configuration
import moe.pure.browser.ui.components.dp

/** qa.e1.b3/v0.j3 use configuration dimensions, not the measured or keyboard-reduced root. */
internal object NativeFavoriteGeometry {
    data class Height(val requested: Int, val target: Int)
    data class Grid(val count: Int, val padding: Int)

    fun height(context: Context, configuration: Configuration, passedHeight: Int): Height {
        val toolbar = context.dp(54)
        val screen = context.dp(configuration.screenHeightDp)
        val requested = if (passedHeight == 0) toolbar + screen / 2 else passedHeight
        return Height(requested, minOf(maxOf(requested, toolbar + context.dp(168)), screen * 3 / 5 + toolbar))
    }

    fun grid(context: Context, width: Int, iconWidth: Int, searchVisible: Boolean, passedCount: Int): Grid {
        val unit = iconWidth + context.dp(18)
        var candidate = context.dp(540) / unit
        val extra = if (searchVisible) 1 else 0
        var result = candidate / 2
        while (candidate >= 0) {
            if (width >= (candidate + extra) * unit) { result = candidate; break }
            candidate--
        }
        val count = maxOf(result, passedCount)
        val padding = (width - unit * count) / 2
        return Grid(count, if (padding > 0) padding else 0)
    }
}
