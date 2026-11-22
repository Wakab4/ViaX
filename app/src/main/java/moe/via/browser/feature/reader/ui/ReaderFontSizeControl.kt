package moe.via.browser.feature.reader.ui

import android.content.Context
import android.content.res.ColorStateList
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

class ReaderFontSizeControl(
    context: Context,
    initialSize: Int,
    private val onSizeChanged: (Int) -> Unit
) : LinearLayout(context) {
    private val slider = SeekBar(context).apply {
        max = 20
        progress = initialSize.coerceIn(10, 30) - 10
        contentDescription = "字体大小"
        progressTintList = ColorStateList.valueOf(context.palette.accent)
        thumbTintList = ColorStateList.valueOf(context.palette.accent)
    }

    init {
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(48)
        setPadding(dp(8), dp(4), dp(8), dp(4))
        addView(button("A-", 15f, -1), LayoutParams(-2, dp(40)))
        addView(slider, LayoutParams(0, -2, 1f))
        addView(button("A+", 18f, 1), LayoutParams(-2, dp(40)))
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) = Unit
            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) { onSizeChanged(seekBar.progress + 10) }
        })
    }

    private fun button(label: String, size: Float, step: Int) = NativeTypography.text(context).apply {
        text = label
        textSize = size
        gravity = Gravity.CENTER
        setPadding(dp(8), 0, dp(8), 0)
        setTextColor(context.palette.text)
        background = context.touchFeedback()
        setOnClickListener {
            slider.progress += step
            onSizeChanged(slider.progress + 10)
        }
    }
}

