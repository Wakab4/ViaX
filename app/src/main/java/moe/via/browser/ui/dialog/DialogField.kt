package moe.via.browser.ui.dialog

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import moe.via.browser.R
import moe.via.browser.ui.theme.palette

object DialogField {
    internal fun createEditor(context: Context, value: String, hint: String = "", lines: Int = 1): EditText =
        NativeTypography.edit(context).apply {
            setText(value); this.hint = hint
            minLines = lines; maxLines = lines; setLines(lines); ellipsize = TextUtils.TruncateAt.END
            inputType = if (lines > 1) 671745 else 524289
            imeOptions = if (lines > 1) EditorInfo.IME_ACTION_NONE else EditorInfo.IME_ACTION_NEXT
            setTextColor(context.palette.text); setHintTextColor(context.palette.secondary)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
            gravity = Gravity.TOP; setSelectAllOnFocus(lines <= 3)
            if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
        }

    fun append(activity: Activity, parent: LinearLayout, value: String, hint: String, lines: Int): EditText {
        fun pixels(dp: Int) = (dp * activity.resources.displayMetrics.density).toInt()
        val editor = createEditor(activity, value, hint, lines)
        if (lines == 1) {
            editor.setPadding(0, 0, 0, 0)
            val wrapper = HorizontalScrollView(activity).apply {
                isFillViewport = true; isHorizontalScrollBarEnabled = false; isVerticalScrollBarEnabled = false
                overScrollMode = android.view.View.OVER_SCROLL_NEVER
                setBackgroundResource(R.drawable.dialog_field_underline); setPadding(0, pixels(8), 0, pixels(8))
                addView(editor, android.widget.FrameLayout.LayoutParams(-2, -2))
            }
            editor.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            parent.addView(wrapper, LinearLayout.LayoutParams(-1, -2).apply { setMargins(pixels(16), 0, pixels(16), pixels(12)) })
        } else {
            editor.setBackgroundResource(R.drawable.dialog_field_underline)
            editor.setPadding(0, pixels(8), 0, pixels(8))
            parent.addView(editor, LinearLayout.LayoutParams(-1, -2).apply { setMargins(pixels(12), 0, pixels(12), pixels(12)) })
        }
        return editor
    }
}
