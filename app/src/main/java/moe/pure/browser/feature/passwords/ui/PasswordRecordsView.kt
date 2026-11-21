package moe.pure.browser.feature.passwords.ui

import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import moe.pure.browser.R
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.components.PageScaffold
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.pure.browser.ui.components.recycler.NativeRecyclerView
import moe.pure.browser.ui.theme.ThemedActivity

internal class PasswordRecordsView(activity: ThemedActivity, applyInsets: Boolean, back: () -> Unit, create: () -> Unit,
    select: () -> Unit, delete: () -> Unit, edit: () -> Unit) : AutoCloseable {
    private val page = PageScaffold(activity, activity.getString(R.string.ref_password_csv_saved), back, applyInsets)
    val search = passwordSearchField(activity)
    val list = NativeRecyclerView(activity).apply {
        id = R.id.settings_page_list; layoutManager = LinearLayoutManager(activity); setHasFixedSize(true)
        clipToPadding = false; NativeRecyclerEdgeEffect.install(this)
    }
    private val group = FrameLayout(activity).apply { addView(list, FrameLayout.LayoutParams(-1, -1)) }
    private val empty = PasswordSettingRows.empty(activity).apply { visibility = View.GONE }
    val footer = PasswordFooterView(activity, select, delete, edit)
    val root get() = page.root
    init {
        val body = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(search, LinearLayout.LayoutParams(-1, -2).apply { marginStart = activity.dp(14); marginEnd = activity.dp(14); topMargin = activity.dp(10); bottomMargin = activity.dp(10) })
            addView(group, LinearLayout.LayoutParams(-1, 0, 1f)); addView(empty, LinearLayout.LayoutParams(-1, 0, 1f)); addView(footer)
        }
        page.showContent(body, bottomInsets = footer)
        page.addIconAction(R.drawable.ref_ic_add, activity.getString(R.string.ref_search_page_new), create)
        NativeToolbarTap.bind(page.toolbar) { list.smoothScrollToPosition(0) }
    }
    fun empty(value: Boolean) { group.visibility = if (value) View.GONE else View.VISIBLE; empty.visibility = if (value) View.VISIBLE else View.GONE }
    override fun close() { list.dispose(); list.adapter = null }
}
