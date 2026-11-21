package moe.pure.browser.ui.settings

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** Shared a6 row geometry; feature owners bind values and actions. */
internal object NativeSettingViews {
    fun label(context: Context, detail: Boolean = false) = NativeTypography.text(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(
            if (detail) R.dimen.ref_redirect_page_detail_size else R.dimen.ref_redirect_page_body_size).toFloat())
        setTextColor(if (detail) context.palette.secondary else context.palette.text)
        maxLines = if (detail) 3 else 1
        ellipsize = TextUtils.TruncateAt.END; textDirection = View.TEXT_DIRECTION_LOCALE
    }

    class Global(context: Context) : RelativeLayout(context) {
        val title = NativeSettingViews.label(context).apply { id = View.generateViewId() }
        val detail = NativeSettingViews.label(context, true)
        val check = CheckBox(context).apply {
            id = View.generateViewId(); isClickable = false; isFocusable = false
            buttonDrawable = context.getDrawable(R.drawable.dialog_checkbox); buttonTintList = null
        }
        init {
            setPaddingRelative(dp(16), dp(20), dp(16), dp(20))
            setBackgroundResource(R.drawable.ref_redirect_page_feedback)
            addView(title, LayoutParams(-1, -2).apply { addRule(ALIGN_PARENT_START); addRule(START_OF, check.id) })
            addView(detail, LayoutParams(-1, -2).apply {
                addRule(ALIGN_PARENT_START); addRule(START_OF, check.id); addRule(BELOW, title.id)
            })
            addView(check, LayoutParams(-2, dp(20)).apply { addRule(ALIGN_PARENT_END); addRule(CENTER_VERTICAL); marginStart = dp(12) })
        }
    }

    class Radio(context: Context) : RelativeLayout(context) {
        val title = label(context).apply { id = View.generateViewId() }
        val detail = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.ref_redirect_page_detail_size).toFloat())
            setTextColor(context.palette.secondary); maxLines = 3
            ellipsize = TextUtils.TruncateAt.END; textDirection = View.TEXT_DIRECTION_LOCALE
        }
        val check = CheckBox(context).apply {
            id = View.generateViewId(); isClickable = false; isFocusable = false
            buttonDrawable = context.getDrawable(R.drawable.dialog_checkbox); buttonTintList = null
        }
        init {
            setPaddingRelative(dp(16), dp(20), dp(16), dp(20))
            setBackgroundResource(R.drawable.ref_redirect_page_feedback)
            addView(title, LayoutParams(-1, -2).apply { addRule(ALIGN_PARENT_END); addRule(END_OF, check.id) })
            addView(detail, LayoutParams(-1, -2).apply {
                addRule(ALIGN_PARENT_END); addRule(END_OF, check.id); addRule(BELOW, title.id)
            })
            addView(check, LayoutParams(-2, dp(20)).apply { addRule(ALIGN_PARENT_START); addRule(CENTER_VERTICAL); marginEnd = dp(18) })
        }
    }

    fun add(context: Context) = label(context).apply {
        setPaddingRelative(dp(16), dp(20), dp(16), dp(20))
        compoundDrawablePadding = dp(18)
        setBackgroundResource(R.drawable.ref_redirect_page_feedback)
    }

    fun section(context: Context) = label(context, true).apply {
        maxLines = Int.MAX_VALUE; ellipsize = null
        setPaddingRelative(dp(16), dp(12), dp(16), dp(4))
        setTextColor(context.palette.accent); setTypeface(typeface, Typeface.BOLD)
    }

    fun site(context: Context) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPaddingRelative(dp(16), dp(20), dp(16), dp(20))
        setBackgroundResource(R.drawable.ref_redirect_page_feedback)
        addView(label(context), LinearLayout.LayoutParams(-1, -2))
        addView(label(context, true).apply { visibility = View.GONE }, LinearLayout.LayoutParams(-2, -2))
    }
}
