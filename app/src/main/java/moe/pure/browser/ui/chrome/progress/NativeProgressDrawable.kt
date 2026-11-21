package moe.pure.browser.ui.chrome.progress

import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.widget.ProgressBar
import moe.pure.browser.ui.components.dp

/** x8.h.x(): the same fixed blue-to-purple strip in both themes, clipped from the left. */
internal object NativeProgressDrawable {
    fun applyTo(bar: ProgressBar) {
        val empty = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
        val fill = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(0xFF59C2E5.toInt(), 0xFFA687E0.toInt())).apply {
            cornerRadius = bar.context.dp(2).toFloat()
        }
        bar.progressTintList = null
        bar.progressBackgroundTintList = null
        bar.secondaryProgressTintList = null
        bar.progressDrawable = LayerDrawable(arrayOf(empty, ClipDrawable(fill, Gravity.LEFT, ClipDrawable.HORIZONTAL))).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
    }
}
