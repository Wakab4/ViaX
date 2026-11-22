package moe.via.browser.feature.adblock.ui

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.via.browser.ui.components.recycler.NativeRecyclerView
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.palette

internal class AdblockListPage(activity: ThemedActivity, title: String, insets: Boolean, back: () -> Unit) : AutoCloseable {
    val page = PageScaffold(activity, title, back, insets)
    val list = NativeRecyclerView(activity).apply {
        id = R.id.settings_page_list; layoutManager = LinearLayoutManager(activity)
        clipToPadding = false; NativeRecyclerEdgeEffect.install(this)
    }
    private val empty = NativeTypography.text(activity).apply {
        text = "¯\\_(ツ)_/¯"; contentDescription = activity.getString(R.string.ref_ai_page_empty)
        textSize = 22f; gravity = Gravity.CENTER; setTextColor(activity.palette.secondary)
        setPadding(0, dp(20), 0, dp(20)); visibility = View.GONE
    }
    val root get() = page.root
    init {
        page.showContent(FrameLayout(activity).apply {
            addView(list, FrameLayout.LayoutParams(-1, -1)); addView(empty, FrameLayout.LayoutParams(-1, -1))
        }, bottomInsets = list)
        NativeToolbarTap.bind(page.toolbar) { list.smoothScrollToPosition(0) }
    }
    fun empty(value: Boolean) {
        list.visibility = if (value) View.GONE else View.VISIBLE
        empty.visibility = if (value) View.VISIBLE else View.GONE
    }
    override fun close() { list.dispose(); list.adapter = null }
}
