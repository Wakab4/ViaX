package moe.via.browser.feature.site.ui.font

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import moe.via.browser.feature.site.ui.common.NativeSiteEntryDialogs
import moe.via.browser.feature.site.ui.common.NativeSiteRow
import moe.via.browser.feature.site.ui.common.NativeSiteSeekBar
import moe.via.browser.ui.settings.NativeSettingViews
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** ib.x/w inline slider and sample. Saving belongs to stop-tracking/focus-loss, not every tick. */
internal class NativeFontRowView(private val activity: Activity) : RelativeLayout(activity) {
    private val title = NativeSettingViews.label(activity).apply { id = android.view.View.generateViewId() }
    private val percentage = NativeSettingViews.label(activity).apply {
        id = android.view.View.generateViewId(); minWidth = dp(32); gravity = Gravity.CENTER
        setTextColor(activity.palette.secondary)
    }
    private val slider = NativeSiteSeekBar(activity)
    private val sample = NativeSettingViews.label(activity).apply {
        text = NativeSiteEntryDialogs.sample(); minHeight = dp(36); maxLines = 3
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = GradientDrawable().apply { setColor(0x30808080); cornerRadius = dp(8).toFloat() }
    }
    init {
        setPaddingRelative(dp(16), dp(20), dp(16), dp(20))
        setBackgroundResource(moe.via.browser.R.drawable.ref_redirect_page_feedback)
        addView(title, LayoutParams(-1, -2))
        addView(percentage, LayoutParams(-2, -2).apply { addRule(ALIGN_PARENT_START); addRule(BELOW, title.id); topMargin = dp(2) })
        addView(slider, LayoutParams(-1, -2).apply {
            addRule(ALIGN_PARENT_END); addRule(END_OF, percentage.id); addRule(ALIGN_TOP, percentage.id)
            topMargin = -dp(3) + (activity.resources.displayMetrics.scaledDensity * 12).toInt() / 2
        })
        addView(sample, LayoutParams(-1, -2).apply { addRule(BELOW, percentage.id); topMargin = dp(12) })
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
    }
    fun bind(row: NativeSiteRow.Slider, save: (Int) -> Unit) {
        title.text = row.title; slider.setOnSeekBarChangeListener(null); slider.onFocusChangeListener = null
        slider.max = 30; slider.highlightProgress = (row.highlight - 50) / 5
        fun update() {
            val value = slider.progress * 5 + 50
            percentage.text = "$value%"
            sample.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                (activity.resources.displayMetrics.scaledDensity * 14 * value / 100).toInt().toFloat())
        }
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) = update()
            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) { save(seekBar.progress * 5 + 50) }
        })
        slider.onFocusChangeListener = android.view.View.OnFocusChangeListener { _, focused -> if (!focused) save(slider.progress * 5 + 50) }
        slider.progress = ((row.value - 50) / 5).coerceIn(0, 30); update()
    }
}
