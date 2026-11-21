package moe.pure.browser.feature.ai.ui.settings

import android.app.Activity
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
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** l1 owns horizontal field input; model/variable lists have their own RecyclerView. */
internal class AiLineField(activity: Activity, hint: Int, type: Int, verticalPadding: Int) : HorizontalScrollView(activity) {
    val editor = AiFields.editor(activity, hint, type).apply {
        setSingleLine(); maxLines = 1; setLines(1); ellipsize = TextUtils.TruncateAt.END
        setPadding(0, 0, 0, 0); setBackgroundColor(0); imeOptions = EditorInfo.IME_ACTION_NEXT
        textDirection = View.TEXT_DIRECTION_LOCALE
    }
    init {
        isFillViewport = true; isHorizontalScrollBarEnabled = false; isVerticalScrollBarEnabled = false
        overScrollMode = if (Build.VERSION.SDK_INT >= 32) View.OVER_SCROLL_IF_CONTENT_SCROLLS else View.OVER_SCROLL_NEVER
        setPadding(0, dp(verticalPadding), 0, dp(verticalPadding)); setBackgroundResource(R.drawable.ref_ai_field_underline)
        addView(editor, FrameLayout.LayoutParams(-2, -2))
    }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean { parent.requestDisallowInterceptTouchEvent(true); return super.dispatchTouchEvent(event) }
}

internal object AiFields {
    fun editor(activity: Activity, hint: Int, type: Int): EditText = NativeTypography.edit(activity).apply {
        inputType = type; gravity = Gravity.CENTER_VERTICAL; setHint(hint)
        setTextColor(activity.palette.text); setHintTextColor(activity.palette.secondary)
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
        NativeTypography.bind(this)
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
    }
    fun margin(activity: Activity, bottom: Int = 12) = LinearLayout.LayoutParams(-1, -2).apply { marginStart = activity.dp(16); marginEnd = activity.dp(16); bottomMargin = activity.dp(bottom) }
}
