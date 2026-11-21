package moe.pure.browser.feature.scripts.ui

import android.graphics.Typeface
import android.os.Build
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.chrome.progress.NativePageProgress
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.scroll.NativeScrollView
import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.ui.theme.palette

internal class ScriptEditorView(activity: ThemedActivity, applyInsets: Boolean, back: () -> Unit, help: () -> Unit, save: () -> Unit) : AutoCloseable {
    val page = PageScaffold(activity, activity.getString(R.string.ref_search_page_edit), back, applyInsets)
    val scroll = NativeScrollView(activity)
    val input = NativeTypography.edit(activity).apply {
        inputType = 655361; setTextColor(activity.palette.text); setHintTextColor(activity.palette.secondary)
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
        gravity = Gravity.TOP or Gravity.LEFT; typeface = Typeface.MONOSPACE; setHint(R.string.ref_script_page_editor_hint)
        textDirection = View.TEXT_DIRECTION_LTR; setBackgroundColor(0); setPaddingRelative(dp(12), dp(16), dp(12), dp(16))
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
    }
    private val bar = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal)
    val progress = NativePageProgress(bar)
    val root get() = page.root
    init {
        page.addTextAction(activity.getString(R.string.ref_script_page_help), help); page.addTextAction(activity.getString(R.string.ref_text_editor_save), save)
        scroll.addView(input, FrameLayout.LayoutParams(-1, -2))
        page.showContent(FrameLayout(activity).apply { addView(scroll, FrameLayout.LayoutParams(-1, -1)); addView(bar, FrameLayout.LayoutParams(-1, activity.dp(3))) })
        NativeToolbarTap.bind(page.toolbar) { scroll.smoothScrollTo(0, 0) }
    }
    override fun close() { scroll.dispose(); progress.dispose() }
}
