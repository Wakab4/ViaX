package moe.via.browser.feature.passwords.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.theme.skin.NativeSkinIcons
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

internal fun passwordSearchField(context: Context): EditText = NativeTypography.edit(context).apply {
    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
    setTextColor(context.palette.text); setHintTextColor(context.palette.hint); setHint(R.string.ref_password_page_search)
    inputType = 1; imeOptions = EditorInfo.IME_ACTION_DONE; textDirection = View.TEXT_DIRECTION_LOCALE
    compoundDrawablePadding = dp(12); setPaddingRelative(dp(16), dp(10), dp(16), dp(10))
    background = GradientDrawable().apply { setColor(0x20808080); cornerRadius = resources.getDimensionPixelSize(R.dimen.ref_reader_color_radius).toFloat() }
    if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
    val clear = (NativeSkinIcons(context).resolve(R.drawable.ref_ic_close, "ic_close") ?: context.getDrawable(R.drawable.ref_ic_close))?.mutate()?.apply { setTint(context.palette.icon); setBounds(0, 0, dp(18), dp(18)) }
    addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) = Unit
        override fun afterTextChanged(value: Editable?) { setCompoundDrawablesRelative(null, null, if (value.isNullOrEmpty()) null else clear, null) }
    })
    var pressed = false
    setOnTouchListener { _, event ->
        if (event.actionMasked == MotionEvent.ACTION_DOWN) pressed = !text.isNullOrEmpty() &&
            if (layoutDirection == View.LAYOUT_DIRECTION_RTL) event.x <= compoundPaddingLeft else event.x >= measuredWidth - compoundPaddingRight
        else if (event.actionMasked == MotionEvent.ACTION_UP && pressed) setText("")
        pressed
    }
}
