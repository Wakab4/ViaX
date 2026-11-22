package moe.via.browser.feature.search.ui.settings

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.RelativeLayout
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.settings.NativeSettingViews
import moe.via.browser.ui.theme.palette

/** ib.r keeps the labels, drag handle and check box in one RelativeLayout. */
internal class SearchToolbarRow(context: Context) : RelativeLayout(context) {
    val title = NativeSettingViews.label(context).apply { id = View.generateViewId(); setLines(1) }
    val detail = NativeSettingViews.label(context, true)
    val handle = ImageView(context).apply {
        id = View.generateViewId(); setImageResource(R.drawable.ref_search_settings_drag)
        imageTintList = ColorStateList.valueOf(context.palette.icon)
        setPaddingRelative(dp(13), 0, dp(13), 0)
    }
    val check = CheckBox(context).apply {
        id = View.generateViewId(); isClickable = false; isFocusable = false
        buttonDrawable = context.getDrawable(R.drawable.dialog_checkbox); buttonTintList = null
    }
    init {
        setPaddingRelative(dp(16), dp(20), dp(16), dp(20)); setBackgroundResource(R.drawable.ref_redirect_page_feedback)
        addView(title, LayoutParams(-1, -2).apply { addRule(ALIGN_PARENT_START); addRule(START_OF, handle.id) })
        addView(detail, LayoutParams(-1, -2).apply { addRule(ALIGN_PARENT_START); addRule(START_OF, handle.id); addRule(BELOW, title.id) })
        addView(handle, LayoutParams(dp(48), -2).apply {
            addRule(START_OF, check.id); addRule(CENTER_VERTICAL); marginStart = dp(16); marginEnd = dp(16)
        })
        addView(check, LayoutParams(-2, dp(20)).apply { addRule(ALIGN_PARENT_END); addRule(CENTER_VERTICAL) })
    }
}
