package moe.via.browser.feature.settings.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** Native ib.i0 preview: a 300 dp browser frame scaled to 80%, without a WebView. */
internal class ToolbarPreview(activity: Activity, layout: Int, tabs: Boolean, address: Int) : FrameLayout(activity) {
    init {
        val shell = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(activity.palette.panel); setStroke(dp(2), activity.palette.border) }
            scaleX = .8f; scaleY = .8f
            clipToOutline = true
        }
        fun icon(id: Int, weight: Float = 1f) = ImageView(activity).apply {
            setImageResource(id); imageTintList = ColorStateList.valueOf(activity.palette.icon)
            scaleType = ImageView.ScaleType.CENTER_INSIDE; setPadding(dp(13), dp(13), dp(13), dp(13))
            layoutParams = LinearLayout.LayoutParams(0, dp(48), weight)
        }
        fun addressBar() = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(icon(R.drawable.ref_ic_menu_site_conf), LinearLayout.LayoutParams(dp(48), dp(48)))
            addView(NativeTypography.text(activity).apply {
                text = when (address) { 1 -> "https://example.com/"; 2 -> "example.com"; else -> "网页" }
                textSize = 16f; setTextColor(activity.palette.text); maxLines = 1
                gravity = Gravity.CENTER_VERTICAL
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
            addView(icon(R.drawable.ref_ic_menu_reload), LinearLayout.LayoutParams(dp(48), dp(48)))
            if (layout != 0) {
                val radius = dp(18).toFloat()
                background = GradientDrawable().apply { cornerRadius = radius; setColor(if (activity.palette.night) 0x22FFFFFF else 0x10000000) }
            }
        }
        fun tabButton() = FrameLayout(activity).apply {
            addView(icon(R.drawable.ref_ic_tabs), LayoutParams(-1, -1))
            addView(NativeTypography.text(activity).apply { text = "1"; textSize = 11f; NativeTypography.style(this, android.graphics.Typeface.BOLD); gravity = Gravity.CENTER; setTextColor(activity.palette.icon) }, LayoutParams(-1, -1))
            layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f)
        }
        fun tools(combined: Boolean) = object : LinearLayout(activity) {
            override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
                super.onSizeChanged(width, height, oldWidth, oldHeight)
                if (!combined || childCount < 6) return
                val widthDp = (width / resources.displayMetrics.density).toInt()
                val unit = when { widthDp < 420 -> widthDp / 7; widthDp > 900 -> 72; else -> 60 }
                val columns = if (unit > 0) widthDp / unit else 6
                val arrows = columns > 8
                getChildAt(1).visibility = if (arrows) View.VISIBLE else View.GONE
                getChildAt(2).visibility = if (arrows) View.VISIBLE else View.GONE
                getChildAt(0).layoutParams = (getChildAt(0).layoutParams as LinearLayout.LayoutParams).apply { weight = (columns - 3 - if (arrows) 2 else 0).coerceAtLeast(1).toFloat() }
            }
        }.apply {
            gravity = Gravity.CENTER_VERTICAL
            if (combined) addView(addressBar(), LinearLayout.LayoutParams(0, -2, 4f))
            addView(icon(R.drawable.ref_ic_back)); addView(icon(R.drawable.ref_ic_forward))
            addView(icon(R.drawable.ref_ic_home))
            addView(tabButton()); addView(icon(R.drawable.ref_ic_menu))
        }
        fun tabBar() = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(NativeTypography.text(activity).apply { text = "  网页   ×"; textSize = 11f; setTextColor(activity.palette.text) }, LinearLayout.LayoutParams(0, dp(36), 1f))
            addView(NativeTypography.text(activity).apply { text = "+"; textSize = 20f; gravity = Gravity.CENTER; setTextColor(activity.palette.icon) }, LinearLayout.LayoutParams(dp(48), dp(36)))
        }
        if (layout == 0) shell.addView(addressBar())
        if (layout == 1) { shell.addView(tools(true)); if (tabs) shell.addView(tabBar()) }
        shell.addView(android.view.View(activity).apply {
            setBackgroundColor(0x18808080)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        if (layout == 2 && tabs) shell.addView(tabBar())
        if (layout == 3) shell.addView(addressBar())
        if (layout != 1) shell.addView(tools(layout == 2))
        addView(shell, LayoutParams(-1, dp(300), Gravity.CENTER))
        isEnabled = false; importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }
}
