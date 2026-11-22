package moe.via.browser.feature.settings.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.theme.palette

internal class NightSettingsPage(private val host: SettingsHost) {
    fun rows(): List<SettingsItem> = listOf(
        SettingsItem.link(1, "网页蒙版滤镜") { filter() },
        host.toggle(2, "强制网页暗色", host.preferences.forceDark, "为网页启用强制暗色，这可能会导致部分页面表现异常") {
            host.preferences.forceDark = it
            GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.WEB_SETTINGS)
        }
    )
    private fun filter() {
        val activity = host.activity
        val body = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val sample = FrameLayout(activity).apply { setPadding(dp(22), dp(16), dp(22), dp(16)) }
        sample.addView(NativeTypography.text(activity).apply {
            text = if (java.util.Locale.getDefault().country.equals("CN", true)) "莫忧世事兼身事，须著人间比梦间。" else "Less is more."
            textSize = 14f
            gravity = Gravity.CENTER; setTextColor(activity.palette.text); setPadding(0, dp(32), 0, dp(32))
        }, FrameLayout.LayoutParams(-1, -2))
        fun preview(alpha: Int) { sample.foreground = GradientDrawable().apply { cornerRadius = activity.dp(18).toFloat(); setColor(Color.argb(alpha, 0, 0, 0)) } }
        preview(host.preferences.nightFilter.coerceIn(0, 154))
        body.addView(sample)
        body.addView(SeekBar(activity).apply {
            max = 154; progress = host.preferences.nightFilter.coerceIn(0, 154)
            setPadding(dp(16), 0, dp(16), dp(16))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, value: Int, fromUser: Boolean) = preview(value)
                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) { host.preferences.nightFilter = bar.progress }
            })
        }, LinearLayout.LayoutParams(-1, -2))
        activity.dialogScope.show(BrowserDialog(activity).custom(body))
    }
}
