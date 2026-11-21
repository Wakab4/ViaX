package moe.pure.browser.feature.search.ui.settings

import androidx.recyclerview.widget.LinearLayoutManager
import moe.pure.browser.R
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.pure.browser.ui.components.recycler.NativeRecyclerView
import moe.pure.browser.ui.theme.ThemedActivity

internal class SearchListPage(activity: ThemedActivity, title: String, applyInsets: Boolean, back: () -> Unit) : AutoCloseable {
    val scaffold = PageScaffold(activity, title, back, applyInsets)
    val list = NativeRecyclerView(activity).apply {
        id = R.id.settings_page_list; layoutManager = LinearLayoutManager(activity); clipToPadding = false
        NativeRecyclerEdgeEffect.install(this)
    }
    val root get() = scaffold.root
    init {
        scaffold.showContent(list, bottomInsets = list)
        NativeToolbarTap.bind(scaffold.toolbar) { list.smoothScrollToPosition(0) }
    }
    override fun close() { list.dispose(); list.adapter = null }
}
