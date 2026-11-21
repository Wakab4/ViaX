package moe.pure.browser.feature.reader.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import moe.pure.browser.R
import moe.pure.browser.feature.reader.ReaderPreferences
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** g8.j calls the consumer before changing only the old and new palette cell backgrounds. */
class ReaderColorPicker(context: Context, initialColor: Int, private val onColorChanged: (Int) -> Unit) : LinearLayout(context) {
    private var selectedIndex = -1
    private val night = context.palette.night
    init {
        gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(12), dp(8), dp(12))
        ReaderPreferences.COLORS.forEachIndexed { index, color ->
            addView(View(context).apply {
                tag = color; background = cell(color, false)
                setOnClickListener { onColorChanged(color); select(index) }
            }, LayoutParams(0, dp(42), 1f).apply { marginStart = dp(8); marginEnd = dp(8) })
        }
        select(ReaderPreferences.COLORS.indexOf(initialColor))
    }
    private fun select(index: Int) {
        if (index == selectedIndex) return
        val previous = selectedIndex; selectedIndex = index
        if (previous in 0 until childCount) getChildAt(previous).let { it.background = cell(it.tag as Int, false) }
        if (index in 0 until childCount) getChildAt(index).let { it.background = cell(it.tag as Int, true) }
    }
    private fun cell(color: Int, selected: Boolean) = GradientDrawable().apply {
        cornerRadius = resources.getDimensionPixelSize(R.dimen.ref_reader_color_radius).toFloat()
        setColor(if (night && color != 0) Color.rgb(
            (Color.red(color) * .3f).toInt(), (Color.green(color) * .3f).toInt(), (Color.blue(color) * .3f).toInt()) else color)
        setStroke(dp(2), if (selected) context.palette.accent else 0x30808080)
    }
}
