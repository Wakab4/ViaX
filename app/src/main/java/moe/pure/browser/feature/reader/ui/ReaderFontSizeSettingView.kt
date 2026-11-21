package moe.pure.browser.feature.reader.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.components.NativeSeekBar
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.settings.NativeSettingViews
import moe.pure.browser.ui.theme.palette
import java.util.Locale

/** ib.x/w/u's value at the start, original seek geometry and live preview; commit on stop or focus loss. */
internal class ReaderFontSizeSettingView(context: Context, initialSize: Int, commit: (Int) -> Unit) : RelativeLayout(context) {
    private val title = NativeSettingViews.label(context).apply { id = TITLE_ID; setText(R.string.ref_reader_settings_font_size); setLines(1) }
    private val value = NativeTypography.text(context).apply {
        id = VALUE_ID; minWidth = dp(32); gravity = Gravity.CENTER; ellipsize = TextUtils.TruncateAt.END
        setTextColor(context.palette.secondary)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
    }
    private val preview = NativeTypography.text(context).apply {
        id = PREVIEW_ID; minHeight = dp(36); maxLines = 3; ellipsize = TextUtils.TruncateAt.END; textDirection = View.TEXT_DIRECTION_LOCALE
        text = if (Locale.getDefault().country.equals("CN", true)) "莫忧世事兼身事，须著人间比梦间。" else "Less is more."
        setTextColor(context.palette.text)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.page_info_title_size).toFloat())
        setPadding(dp(16), 0, dp(16), 0)
        background = GradientDrawable().apply { setColor(0x30808080); cornerRadius = resources.getDimensionPixelSize(R.dimen.ref_reader_preview_radius).toFloat() }
    }
    private val slider = NativeSeekBar(context).apply { id = SLIDER_ID }
    init {
        setPaddingRelative(dp(16), dp(20), dp(16), dp(20)); setBackgroundResource(R.drawable.ref_redirect_page_feedback)
        descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        addView(title, LayoutParams(-1, -2))
        addView(value, LayoutParams(-2, -2).apply { addRule(ALIGN_PARENT_START); addRule(BELOW, title.id); topMargin = dp(2) })
        addView(slider, LayoutParams(-1, -2).apply {
            addRule(ALIGN_PARENT_END); addRule(END_OF, value.id); addRule(ALIGN_TOP, value.id)
            topMargin = -dp(3) + resources.getDimensionPixelSize(R.dimen.ref_redirect_page_detail_size) / 2
        })
        addView(preview, LayoutParams(-1, -2).apply { addRule(BELOW, value.id); topMargin = dp(12) })
        slider.max = 20
        val progress = initialSize - 10
        if (progress == 0) slider.progress = 1
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            @Suppress("DEPRECATION")
            override fun onProgressChanged(view: SeekBar, progress: Int, fromUser: Boolean) {
                val size = progress + 10
                val pixels = (14f * resources.displayMetrics.scaledDensity + .5f).toInt().toDouble() * size / 17
                preview.setTextSize(TypedValue.COMPLEX_UNIT_PX, pixels.toInt().toFloat())
                value.text = String.format(Locale.ROOT, "%dpx", size)
            }
            override fun onStartTrackingTouch(view: SeekBar) = Unit
            override fun onStopTrackingTouch(view: SeekBar) { commit(view.progress + 10) }
        })
        slider.setOnFocusChangeListener { view, focused -> if (!focused) commit((view as SeekBar).progress + 10) }
        slider.progress = progress
    }
    companion object {
        private val TITLE_ID = View.generateViewId()
        private val SLIDER_ID = View.generateViewId()
        private val VALUE_ID = View.generateViewId()
        private val PREVIEW_ID = View.generateViewId()
    }
}
