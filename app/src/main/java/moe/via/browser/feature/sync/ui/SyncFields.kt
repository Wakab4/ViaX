package moe.via.browser.feature.sync.ui

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.editor.NativePasswordVisibility
import moe.via.browser.ui.theme.palette

internal object SyncFields {
    fun field(activity: Activity, hint: String, value: String = "", secret: Boolean = false): EditText = NativeTypography.edit(activity).apply {
        id = View.generateViewId(); setText(value); this.hint = hint
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
        setTextColor(activity.palette.text); setHintTextColor(activity.palette.secondary)
        setBackgroundResource(R.drawable.dialog_field_underline); setSingleLine(true); setHorizontallyScrolling(true)
        setSelectAllOnFocus(true); setPadding(0, activity.dp(8), 0, activity.dp(8))
        inputType = 524289
        imeOptions = EditorInfo.IME_ACTION_NEXT; textDirection = View.TEXT_DIRECTION_LOCALE
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
        if (secret) NativePasswordVisibility.attach(this)
    }
    fun append(parent: LinearLayout, field: EditText, bottom: Int = 12) {
        parent.addView(field, LinearLayout.LayoutParams(-1, -2).apply {
            marginStart = parent.dp(16); marginEnd = parent.dp(16); bottomMargin = parent.dp(bottom)
        })
    }
}
