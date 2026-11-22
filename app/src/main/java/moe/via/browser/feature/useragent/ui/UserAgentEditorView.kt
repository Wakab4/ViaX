package moe.via.browser.feature.useragent.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** hb.v1/l1: a horizontally scrolling name and a fixed five-line LTR user-agent field. */
internal class UserAgentEditorView(context: Context) : LinearLayout(context) {
    val name = NativeTypography.edit(context).apply {
        id = R.id.user_agent_editor_name; inputType = 1; imeOptions = EditorInfo.IME_ACTION_NEXT
        setTextColor(context.palette.text); setHintTextColor(context.palette.secondary); textSize = 14f
        setHint(R.string.ref_ua_name_hint); setBackgroundColor(0); setPadding(0, 0, 0, 0)
        setSingleLine(true); setLines(1); maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
        textDirection = View.TEXT_DIRECTION_LOCALE
    }
    val value = NativeTypography.edit(context).apply {
        id = R.id.user_agent_editor_value; inputType = 524289; imeOptions = EditorInfo.IME_ACTION_DONE
        setLines(5); maxLines = 5; minLines = 5; gravity = Gravity.TOP
        setHorizontallyScrolling(false); textDirection = View.TEXT_DIRECTION_LTR
        setTextColor(context.palette.text); setHintTextColor(context.palette.secondary); textSize = 14f
        setHint(R.string.ref_ua_title); setPadding(0, dp(8), 0, dp(8))
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
        setBackgroundResource(R.drawable.ref_ua_editor_line)
    }
    init {
        orientation = VERTICAL; setPaddingRelative(dp(16), dp(12), dp(16), 0)
        addView(object : HorizontalScrollView(context) {
            init {
                isHorizontalScrollBarEnabled = false; isVerticalScrollBarEnabled = false; isFillViewport = true
                overScrollMode = if (Build.VERSION.SDK_INT >= 32) OVER_SCROLL_IF_CONTENT_SCROLLS else OVER_SCROLL_NEVER
                setPaddingRelative(0, dp(8), 0, dp(8)); setBackgroundResource(R.drawable.ref_ua_editor_line)
                addView(name, FrameLayout.LayoutParams(-2, -2))
            }
            override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                parent?.requestDisallowInterceptTouchEvent(true)
                return super.dispatchTouchEvent(event)
            }
        }, LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        addView(value, LayoutParams(-1, -2))
    }
}
