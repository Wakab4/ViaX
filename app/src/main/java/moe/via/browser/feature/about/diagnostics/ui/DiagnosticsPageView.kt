package moe.via.browser.feature.about.diagnostics.ui

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.scroll.NativeScrollView
import moe.via.browser.ui.components.toolbar.NativeToolbarAction
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.palette

internal class DiagnosticsPageView(activity: ThemedActivity, applyInsets: Boolean, back: () -> Unit,
    delete: () -> Unit, share: () -> Unit, bottom: () -> Unit) {
    private val page = PageScaffold(activity, activity.getString(R.string.ref_diagnostics_title), back, applyInsets)
    private val text = TextView(activity).apply {
        typeface = Typeface.MONOSPACE
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.settings_detail_size).toFloat())
        setTextColor(activity.palette.text)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setTextIsSelectable(true); textDirection = View.TEXT_DIRECTION_LTR
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
    }
    private val scroll = TailScroll(activity, bottom).apply { addView(text, FrameLayout.LayoutParams(-1, -2)) }
    val root get() = page.root

    init {
        page.toolbar.addAction(NativeToolbarAction(View.generateViewId(), 0, R.drawable.ref_diag_delete,
            activity.getString(R.string.ref_diagnostics_delete), skinKey = "ic_delete"), delete)
        page.toolbar.addAction(NativeToolbarAction(View.generateViewId(), 0, R.drawable.ref_diag_share,
            activity.getString(R.string.ref_diagnostics_share), skinKey = "ic_share"), share)
        NativeToolbarTap.bind(page.toolbar) { scroll.smoothScrollTo(0, 0) }
        page.showContent(FrameLayout(activity).apply { addView(scroll, FrameLayout.LayoutParams(-2, -2)) })
    }

    fun actions(visible: Boolean) = page.toolbar.setGroupVisible(0, visible)
    fun replace(value: CharSequence) { text.text = value }
    fun append(value: CharSequence) { text.setTextIsSelectable(false); text.append(value); text.setTextIsSelectable(true) }
    fun close() = scroll.dispose()

    private class TailScroll(context: Context, private val reached: () -> Unit) : NativeScrollView(context) {
        override fun onScrollChanged(x: Int, y: Int, oldX: Int, oldY: Int) {
            super.onScrollChanged(x, y, oldX, oldY)
            if (childCount > 0 && getChildAt(childCount - 1).bottom - (height + scrollY) == 0) reached()
        }
    }
}
