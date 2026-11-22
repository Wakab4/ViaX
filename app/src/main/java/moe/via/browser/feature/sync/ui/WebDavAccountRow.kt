package moe.via.browser.feature.sync.ui

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import moe.via.browser.R
import moe.via.browser.ui.theme.skin.NativeSkinIcons
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.settings.NativeSettingViews
import moe.via.browser.ui.theme.palette

/** a6.b/c owns the account label and the independent reload action. */
internal class WebDavAccountRow(context: Context) : RelativeLayout(context) {
    val title = NativeSettingViews.label(context).apply { id = View.generateViewId() }
    val detail = NativeSettingViews.label(context, true).apply { id = View.generateViewId() }
    val refresh = ImageView(context).apply {
        id = View.generateViewId(); imageTintList = ColorStateList.valueOf(context.palette.icon)
        setImageDrawable(NativeSkinIcons(context).resolve(R.drawable.ref_sync_refresh, "ic_reload") ?: context.getDrawable(R.drawable.ref_sync_refresh))
        setPadding(dp(8), dp(8), dp(8), dp(8)); maxHeight = dp(48)
        setBackgroundResource(R.drawable.dialog_button_feedback)
    }
    init {
        val inset = dp(16)
        setPaddingRelative(inset, dp(20), inset / 5, dp(20)); setBackgroundResource(R.drawable.ref_redirect_page_feedback)
        addView(title, LayoutParams(-1, -2).apply { addRule(ALIGN_PARENT_START); addRule(START_OF, refresh.id) })
        addView(detail, LayoutParams(-1, -2).apply { addRule(ALIGN_PARENT_START); addRule(START_OF, refresh.id); addRule(BELOW, title.id) })
        addView(refresh, LayoutParams(dp(48), -2).apply {
            addRule(ALIGN_PARENT_END); addRule(ALIGN_TOP, title.id); addRule(ALIGN_BOTTOM, detail.id); marginStart = dp(12)
        })
    }
}
