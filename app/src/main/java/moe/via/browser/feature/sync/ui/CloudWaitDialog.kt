package moe.via.browser.feature.sync.ui

import android.app.Activity
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.theme.palette

/** z8.u0/a4 use one noncancelable wait message without a title or cancellation action. */
internal object CloudWaitDialog {
    fun create(activity: Activity) = BrowserDialog(activity).custom(LinearLayout(activity).apply {
        gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(20), dp(16), dp(20))
        addView(ProgressBar(activity), LinearLayout.LayoutParams(dp(32), dp(32)))
        addView(NativeTypography.text(activity).apply {
            setText(R.string.ref_sync_page_wait); setTextColor(activity.palette.text)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(16) })
    }).apply { setCancelable(false); setCanceledOnTouchOutside(false) }
}
