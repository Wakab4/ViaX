package moe.pure.browser.feature.search.ui.settings

import android.content.Context
import android.os.Build
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

internal class SearchEngineEditorFields(context: Context, builtin: Boolean) {
    val root = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), 0) }
    val title = NativeTypography.edit(context).apply {
        setSingleLine(); maxLines = 1; setLines(1); ellipsize = TextUtils.TruncateAt.END
        inputType = 1; imeOptions = EditorInfo.IME_ACTION_NEXT; setHint(R.string.ref_search_page_title_hint)
        setBackgroundColor(0); setPadding(0, 0, 0, 0)
        setTextColor(if (builtin) context.palette.secondary else context.palette.text); setHintTextColor(context.palette.secondary)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.ref_text_editor_size).toFloat())
        isEnabled = !builtin; textDirection = View.TEXT_DIRECTION_LOCALE
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
    }
    val address = NativeTypography.edit(context).apply {
        inputType = 524289; imeOptions = EditorInfo.IME_ACTION_DONE; setLines(5); maxLines = 5; minLines = 5
        setHorizontallyScrolling(false); gravity = Gravity.TOP; textDirection = View.TEXT_DIRECTION_LTR
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.ref_text_editor_size).toFloat()); setHint(R.string.ref_search_page_address_hint)
        setTextColor(context.palette.text); setHintTextColor(context.palette.secondary)
        setPadding(0, dp(8), 0, dp(8)); setBackgroundResource(R.drawable.ref_search_settings_field)
        visibility = if (builtin) View.GONE else View.VISIBLE
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
    }
    val shortcut = NativeTypography.edit(context).apply {
        setSingleLine(); inputType = 524289; imeOptions = EditorInfo.IME_ACTION_DONE
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.ref_text_editor_size).toFloat()); setHint(R.string.ref_search_page_shortcut_hint)
        setTextColor(context.palette.text); setHintTextColor(context.palette.secondary)
        setPaddingRelative(paddingStart, dp(8), paddingEnd, dp(8)); setBackgroundResource(R.drawable.ref_search_settings_field)
    }
    init {
        val titleHost = object : HorizontalScrollView(context) {
            override fun dispatchTouchEvent(event: MotionEvent): Boolean { parent.requestDisallowInterceptTouchEvent(true); return super.dispatchTouchEvent(event) }
        }.apply {
            isFillViewport = true; isHorizontalScrollBarEnabled = false; isVerticalScrollBarEnabled = false
            overScrollMode = if (Build.VERSION.SDK_INT >= 32) View.OVER_SCROLL_IF_CONTENT_SCROLLS else View.OVER_SCROLL_NEVER
            setPadding(0, dp(8), 0, dp(8)); setBackgroundResource(R.drawable.ref_search_settings_field)
            addView(title, FrameLayout.LayoutParams(-2, -2))
        }
        root.addView(titleHost, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = context.dp(12) })
        root.addView(address, LinearLayout.LayoutParams(-1, -2))
        root.addView(shortcut, LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(12) })
        root.addView(NativeTypography.text(context).apply {
            setText(R.string.ref_search_page_shortcut_help)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.ref_redirect_page_detail_size).toFloat()); setTextColor(context.palette.secondary)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = context.dp(6) })
    }
}
