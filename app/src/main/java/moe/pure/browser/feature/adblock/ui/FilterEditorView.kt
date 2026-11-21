package moe.pure.browser.feature.adblock.ui

import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.scroll.NativeScrollView
import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.ui.theme.palette

internal class FilterEditorView(activity: ThemedActivity, insets: Boolean, back: () -> Unit, help: () -> Unit, save: () -> Unit) : AutoCloseable {
    val page = PageScaffold(activity, activity.getString(R.string.ref_adblock_page_custom_rules), back, insets)
    private val scroll = NativeScrollView(activity).apply { isFillViewport = true; clipToPadding = false }
    val input = NativeTypography.edit(activity).apply {
        inputType = 655361; setTextColor(activity.palette.text); setHintTextColor(activity.palette.secondary)
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.ref_redirect_page_body_size).toFloat())
        gravity = Gravity.TOP; typeface = Typeface.MONOSPACE; setHint(R.string.ref_adblock_page_rules_hint)
        textDirection = View.TEXT_DIRECTION_LTR; setBackgroundColor(android.graphics.Color.TRANSPARENT)
        setPadding(dp(12), dp(16), dp(12), dp(16))
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
    }
    val root get() = page.root
    init {
        scroll.addView(input, FrameLayout.LayoutParams(-1, -2)); page.showContent(scroll)
        page.addTextAction(activity.getString(R.string.ref_adblock_page_help), help)
        page.addTextAction(activity.getString(R.string.ref_adblock_page_save), save)
        NativeToolbarTap.bind(page.toolbar) { scroll.smoothScrollTo(0, 0) }
    }
    override fun close() = scroll.dispose()
}
