package moe.pure.browser.feature.about.ui

import androidx.recyclerview.widget.LinearLayoutManager
import moe.pure.browser.R
import moe.pure.browser.feature.settings.SettingsItem
import moe.pure.browser.feature.settings.ui.SettingsRowAdapter
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.pure.browser.ui.components.recycler.NativeRecyclerView
import moe.pure.browser.ui.theme.ThemedActivity

/** Both hb.d and hb.j4 use o8.g's actual recycled list, with their own item animation choice. */
internal class AboutListPage(activity: ThemedActivity, title: String, applyInsets: Boolean,
    animateItems: Boolean, back: () -> Unit) : AutoCloseable {
    private val page = PageScaffold(activity, title, back, applyInsets)
    private val adapter = SettingsRowAdapter(activity) {}
    private val list = NativeRecyclerView(activity).apply {
        id = R.id.settings_page_list
        layoutManager = LinearLayoutManager(activity)
        clipToPadding = false
        NativeRecyclerEdgeEffect.install(this)
        if (!animateItems) itemAnimator = null
        adapter = this@AboutListPage.adapter
    }
    val root get() = page.root
    init {
        page.showContent(list, bottomInsets = list)
        NativeToolbarTap.bind(page.toolbar) { list.smoothScrollToPosition(0) }
    }
    fun rows(value: List<SettingsItem>) = adapter.submit(value)
    override fun close() { list.dispose(); list.adapter = null }
}
