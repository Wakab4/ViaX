package moe.pure.browser.feature.passwords.ui

import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette
import moe.pure.browser.ui.editor.NativePasswordVisibility

/** va.s/l1: ordinary platform fields, including Native's explicit password autofill hint. */
internal class PasswordEditorFields(context: Context) {
    val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(12), 0, 0) }
    val site = line(context, R.string.ref_search_page_address_hint)
    val username = line(context, R.string.ref_password_page_username)
    val password = NativeTypography.edit(context).apply {
        setLines(1); minLines = 1; maxLines = 1; inputType = 129; imeOptions = EditorInfo.IME_ACTION_NEXT
        setAutofillHints("password"); setHint(R.string.ref_password_page_password); gravity = Gravity.CENTER_VERTICAL
        style(this); setPaddingRelative(paddingStart, dp(10), paddingEnd, dp(10)); setBackgroundResource(R.drawable.ref_ai_field_underline)
        NativePasswordVisibility.attach(this)
    }
    val note = NativeTypography.edit(context).apply {
        inputType = 655361; setLines(5); minLines = 5; maxLines = 5; gravity = Gravity.TOP
        textDirection = View.TEXT_DIRECTION_LTR; setHorizontallyScrolling(false); setHint(R.string.ref_password_page_note)
        style(this); setPaddingRelative(paddingStart, dp(10), paddingEnd, dp(10)); setBackgroundResource(R.drawable.ref_ai_field_underline)
    }
    init {
        listOf(site, username).forEach { editor ->
            val host = object : HorizontalScrollView(context) {
                override fun dispatchTouchEvent(event: MotionEvent): Boolean { parent.requestDisallowInterceptTouchEvent(true); return super.dispatchTouchEvent(event) }
            }.apply {
                isFillViewport = true; isHorizontalScrollBarEnabled = false; isVerticalScrollBarEnabled = false
                overScrollMode = if (Build.VERSION.SDK_INT >= 32) View.OVER_SCROLL_IF_CONTENT_SCROLLS else View.OVER_SCROLL_NEVER
                setPadding(0, dp(10), 0, dp(10)); setBackgroundResource(R.drawable.ref_ai_field_underline)
                addView(editor, FrameLayout.LayoutParams(-2, -2))
            }
            root.addView(host, margins(context))
        }
        root.addView(password, margins(context)); root.addView(note, margins(context))
    }
    fun editors() = listOf(site, username, password, note)
    fun values() = editors().map { it.text.toString().trim { character -> character <= ' ' } }
    private fun line(context: Context, caption: Int) = NativeTypography.edit(context).apply {
        setSingleLine(); maxLines = 1; setLines(1); ellipsize = android.text.TextUtils.TruncateAt.END
        inputType = 524289; imeOptions = EditorInfo.IME_ACTION_NEXT; setHint(caption)
        gravity = Gravity.CENTER_VERTICAL; textDirection = View.TEXT_DIRECTION_LOCALE
        setBackgroundColor(0); setPadding(0, 0, 0, 0); style(this)
    }
    private fun style(editor: EditText) {
        editor.setTextColor(editor.context.palette.text); editor.setHintTextColor(editor.context.palette.secondary)
        editor.setTextSize(TypedValue.COMPLEX_UNIT_PX, editor.resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
        NativeTypography.bind(editor)
        if (Build.VERSION.SDK_INT >= 35) editor.setLocalePreferredLineHeightForMinimumUsed(false)
    }
    private fun margins(context: Context) = LinearLayout.LayoutParams(-1, -2).apply { marginStart = context.dp(16); marginEnd = context.dp(16); bottomMargin = context.dp(12) }
}
