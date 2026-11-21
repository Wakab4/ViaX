package moe.pure.browser.ui.editor

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import moe.pure.browser.R
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** Native code-editor geometry shared by rule and userscript editors. */
class CodeEditorPage(activity: Activity, title: String, initial: String, hint: String = "", back: () -> Unit, help: () -> Unit, save: () -> Unit) {
    val scaffold = PageScaffold(activity, title, back)
    val input = NativeTypography.edit(activity).apply {
        setText(initial); this.hint = hint; setTextColor(activity.palette.text); setHintTextColor(activity.palette.secondary)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        typeface = Typeface.MONOSPACE; gravity = Gravity.TOP; textDirection = View.TEXT_DIRECTION_LTR
        setBackgroundColor(Color.TRANSPARENT); setPadding(dp(12), dp(16), dp(12), dp(16))
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
        isHorizontalScrollBarEnabled = false
    }
    init { scaffold.addTextAction("帮助", help); scaffold.addTextAction("保存", save); scaffold.body.addView(input, LinearLayout.LayoutParams(-1, -1)) }
}
