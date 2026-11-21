package moe.pure.browser.ui.editor

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.insets.NativePageInsets
import moe.pure.browser.ui.components.scroll.NativeScrollView
import moe.pure.browser.ui.components.toolbar.NativePageToolbar
import moe.pure.browser.ui.theme.palette

/** hb.i1's scroll container and text field, shared by its actual feature callers. */
internal class NativeTextEditorPage(activity: Activity, title: String?, text: String?, placeholder: String?,
    code: Boolean, applyInsets: Boolean, back: () -> Unit, save: () -> Unit) {
    val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(activity.palette.canvas) }
    val scroll = NativeScrollView(activity)
    val input = NativeTypography.edit(activity).apply {
        setBackgroundColor(Color.TRANSPARENT); setPadding(dp(16), dp(16), dp(16), dp(16))
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        inputType = 131073
        setTextColor(activity.palette.text); setHintTextColor(activity.palette.secondary)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.ref_text_editor_size).toFloat())
        gravity = Gravity.TOP
        setText(text); hint = placeholder
        textDirection = if (code) View.TEXT_DIRECTION_LOCALE else View.TEXT_DIRECTION_LTR
        if (code) { inputType = inputType or 524288; typeface = Typeface.MONOSPACE }
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
    }
    init {
        val toolbar = NativePageToolbar(activity, title.orEmpty(), back)
        toolbar.addTextAction(activity.getString(R.string.ref_text_editor_save), save)
        NativeToolbarTap.bind(toolbar) { scroll.smoothScrollTo(0, 0) }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, -2))
        scroll.addView(input, FrameLayout.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        if (applyInsets) NativePageInsets(root, toolbar)
    }
    fun close() = scroll.dispose()
}
