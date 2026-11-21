package moe.pure.browser.feature.passwords.ui

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

internal object PasswordSettingRows {
    fun empty(context: Context) = NativeTypography.text(context).apply {
        text = "¯\\_(ツ)_/¯"; contentDescription = context.getString(R.string.ref_password_page_empty_description)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.ref_password_page_empty_size).toFloat())
        gravity = Gravity.CENTER; setTextColor(context.palette.secondary); setPadding(0, dp(20), 0, dp(20))
    }
}
