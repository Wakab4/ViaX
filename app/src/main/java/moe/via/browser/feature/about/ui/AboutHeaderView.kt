package moe.via.browser.feature.about.ui

import android.content.Context
import android.graphics.Typeface
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

internal data class AboutHeader(val name: String, val version: String)

/** The logo opens diagnostics; the app name and version are display-only. */
internal class AboutHeaderView(context: Context, header: AboutHeader, openDiagnostics: () -> Unit) : LinearLayout(context) {
    init {
        orientation = VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
        setPadding(0, dp(24), 0, dp(24))
        addView(ImageView(context).apply {
            setImageResource(R.drawable.ref_about_logo)
            contentDescription = context.getString(R.string.ref_about_page_diagnostics)
            var lastTapAt: Long? = null
            setOnClickListener {
                val now = SystemClock.elapsedRealtime()
                if (lastTapAt == null || now - lastTapAt!! > 300L) {
                    lastTapAt = now
                    openDiagnostics()
                }
            }
        }, LayoutParams(dp(60), dp(60)))
        addView(NativeTypography.text(context).apply {
            text = header.name; setTextColor(context.palette.text)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.page_info_title_size).toFloat())
            typeface = Typeface.DEFAULT_BOLD
        }, LayoutParams(-2, -2).apply { topMargin = dp(12) })
        addView(NativeTypography.text(context).apply {
            text = header.version; setTextColor(context.palette.text)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
            typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        }, LayoutParams(-2, -2).apply { topMargin = dp(2) })
    }
}
