package moe.pure.browser.feature.readaloud.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette
import java.util.Locale

/** The reference swaps four transport cells for one slider and retains the fifth speed cell. */
internal class ReadAloudSpeedControl(
    context: Context,
    private val onRateCommitted: (Float) -> Unit
) {
    val label = NativeTypography.text(context).apply {
        gravity = Gravity.CENTER
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
            resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
        setTextColor(context.palette.secondary)
        setBackgroundResource(R.drawable.ref_tts_feedback)
        contentDescription = "朗读语速"
    }
    val slider = (LayoutInflater.from(context).inflate(R.layout.ref_tts_speed, null) as SeekBar).apply {
        max = 13
        visibility = View.GONE
        contentDescription = "朗读语速"
        splitTrack = false
        progressDrawable = track(context)
        thumb = GradientDrawable().apply {
            cornerRadius = context.dp(6).toFloat()
            setColor(context.palette.accent)
            setSize(context.dp(12), context.dp(12))
        }
    }
    private var expanded = false
    private var tracking = false
    private var displayedRate = 1f

    init {
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) { tracking = true }
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) showRate((progress + 1) * .25f)
            }
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                tracking = false
                val rate = (seekBar.progress + 1) * .25f
                showRate(rate)
                onRateCommitted(rate)
            }
        })
    }

    fun setRate(rate: Float) {
        if (tracking) return
        slider.progress = ((rate / .25f).toInt() - 1).coerceIn(0, 13)
        showRate(rate)
    }

    fun install(container: LinearLayout, transport: List<View>) {
        label.setOnClickListener {
            expanded = !expanded
            NativeTypography.style(label, if (expanded) Typeface.BOLD else Typeface.NORMAL)
            label.setTextColor(if (expanded) label.context.palette.accent else label.context.palette.secondary)
            (transport + slider).forEach { child ->
                val visible = if (child === slider) expanded else !expanded
                child.animate().cancel()
                if (visible) {
                    child.visibility = View.VISIBLE
                    child.alpha = 0f
                    child.animate().alpha(1f).setDuration(200).start()
                } else child.visibility = View.GONE
            }
            container.requestLayout()
        }
    }

    private fun showRate(rate: Float) {
        displayedRate = rate
        label.text = String.format(Locale.ROOT, "%.2fx", displayedRate)
    }

    private fun track(context: Context): LayerDrawable {
        fun shape(color: Int) = GradientDrawable().apply {
            cornerRadius = context.dp(5).toFloat()
            setColor(color)
            setSize(0, context.dp(2))
        }
        return LayerDrawable(arrayOf(shape(0x40808080),
            ClipDrawable(shape(context.palette.accent), Gravity.LEFT, ClipDrawable.HORIZONTAL))).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
    }
}
