package moe.via.browser.feature.passwords.ui

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

internal object PasswordSettingRows {
    fun empty(context: Context) = NativeTypography.text(context).apply {
        text = "¯\\_(ツ)_/¯"; contentDescription = context.getString(R.string.ref_password_page_empty_description)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.ref_password_page_empty_size).toFloat())
        gravity = Gravity.CENTER; setTextColor(context.palette.secondary); setPadding(0, dp(20), 0, dp(20))
    }
}
