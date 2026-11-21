package moe.pure.browser.feature.library.history.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp

/** e6.a's 48 dp tail has no label, click handler or retry action. READY keeps the blank row. */
internal class HistoryTailView(context: Context) : FrameLayout(context) {
    private val progress = ProgressBar(context).apply {
        isIndeterminate = false
        indeterminateDrawable = context.getDrawable(R.drawable.ref_history_loading)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    init {
        layoutParams = RecyclerView.LayoutParams(-1, dp(48))
        addView(progress, LayoutParams(-1, -1, Gravity.CENTER))
    }
    fun render(state: HistoryTailState) { progress.visibility = if (state == HistoryTailState.LOADING) View.VISIBLE else View.GONE }
}
