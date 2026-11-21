package moe.pure.browser.feature.scripts.popup.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.Typeface
import android.view.TouchDelegate
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.touchFeedback
import moe.pure.browser.ui.theme.palette

internal object ScriptPopupViews {
    fun text(context: Context) = NativeTypography.text(context).apply {
        setTextColor(context.palette.text)
        textDirection = View.TEXT_DIRECTION_LOCALE
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
        setSingleLine(true); ellipsize = null; setFadingEdgeLength(dp(24)); isHorizontalFadingEdgeEnabled = true
    }
    class CheckRow(context: Context) : RelativeLayout(context) {
        val title = text(context).apply { id = View.generateViewId() }
        val check = CheckBox(context).apply {
            id = View.generateViewId(); buttonDrawable = context.getDrawable(R.drawable.dialog_checkbox); buttonTintList = null
            minimumWidth = 0; minimumHeight = 0; setPadding(0, 0, 0, 0); background = null
            isClickable = true; isFocusable = true
        }
        init {
            setPadding(dp(16), dp(16), dp(16), dp(16)); background = context.touchFeedback()
            addView(check, LayoutParams(-2, dp(20)).apply { addRule(ALIGN_PARENT_END); addRule(CENTER_VERTICAL); marginStart = dp(12) })
            addView(title, LayoutParams(-1, -2).apply { addRule(START_OF, check.id); addRule(ALIGN_PARENT_START) })
            post { val bounds = Rect(); check.getHitRect(bounds); bounds.inset(-dp(24), -dp(24)); touchDelegate = TouchDelegate(bounds, check) }
        }
    }
    class Header(context: Context, detail: Boolean) : RelativeLayout(context) {
        val title = text(context).apply {
            id = View.generateViewId(); NativeTypography.style(this, android.graphics.Typeface.BOLD)
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.page_info_title_size).toFloat())
            ellipsize = android.text.TextUtils.TruncateAt.END; isHorizontalFadingEdgeEnabled = false
            if (detail) setPadding(dp(6), dp(16), dp(16), dp(16)) else setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        val action = ImageView(context).apply {
            id = View.generateViewId(); imageTintList = ColorStateList.valueOf(context.palette.icon)
            setPadding(dp(13), dp(13), dp(13), dp(13)); background = context.touchFeedback()
        }
        init {
            addView(action, LayoutParams(dp(48), -2).apply {
                addRule(if (detail) ALIGN_PARENT_START else ALIGN_PARENT_END)
                addRule(ALIGN_TOP, title.id); addRule(ALIGN_BOTTOM, title.id)
            })
            addView(title, LayoutParams(-1, -2).apply { addRule(if (detail) END_OF else START_OF, action.id) })
        }
    }
}

/** Native SizedLinearLayoutManager clamps the measured list, not the whole dialog window. */
internal class ScriptPopupLayoutManager(context: Context, private val detail: Boolean) : LinearLayoutManager(context) {
    private val display = context.resources.displayMetrics
    override fun setMeasuredDimension(width: Int, height: Int) {
        val maximum = minOf(display.heightPixels / 2, (600 * display.density + .5f).toInt())
        val minimum = minOf(display.heightPixels / if (detail) 4 else 5, (200 * display.density + .5f).toInt())
        super.setMeasuredDimension(width, maxOf(height, minimum).coerceAtMost(maximum))
    }
}
