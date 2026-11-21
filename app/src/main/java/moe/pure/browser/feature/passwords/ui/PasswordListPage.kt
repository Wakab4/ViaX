package moe.pure.browser.feature.passwords.ui

import android.view.View
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import moe.pure.browser.R
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.pure.browser.ui.components.recycler.NativeRecyclerView
import moe.pure.browser.ui.theme.ThemedActivity

internal class PasswordListPage(activity: ThemedActivity, title: String, applyInsets: Boolean, back: () -> Unit) : AutoCloseable {
    val page = PageScaffold(activity, title, back, applyInsets)
    val list = NativeRecyclerView(activity).apply {
        id = R.id.settings_page_list; layoutManager = LinearLayoutManager(activity); clipToPadding = false
        NativeRecyclerEdgeEffect.install(this)
    }
    private val empty = PasswordSettingRows.empty(activity).apply { visibility = View.GONE }
    val root get() = page.root
    init {
        val body = FrameLayout(activity).apply { addView(list, FrameLayout.LayoutParams(-1, -1)); addView(empty, FrameLayout.LayoutParams(-1, -1)) }
        page.showContent(body, bottomInsets = list)
        NativeToolbarTap.bind(page.toolbar) { list.smoothScrollToPosition(0) }
    }
    fun empty(value: Boolean) { list.visibility = if (value) View.GONE else View.VISIBLE; empty.visibility = if (value) View.VISIBLE else View.GONE }
    override fun close() { list.dispose(); list.adapter = null }
}
