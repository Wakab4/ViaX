package moe.via.browser.browser.ui.tabs.closed

import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import moe.via.browser.R
import moe.via.browser.browser.session.ClosedTab
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.feature.library.LibraryEntry
import moe.via.browser.feature.library.LibraryKind
import moe.via.browser.feature.library.ui.NativeLibrarySearch
import moe.via.browser.feature.library.ui.recycler.LibraryDateDecoration
import moe.via.browser.feature.library.ui.recycler.LibraryRecyclerView
import moe.via.browser.ui.components.DividerLayout
import moe.via.browser.ui.components.NativeToolbarTap
import moe.via.browser.ui.components.PageScaffold
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.palette

/** pc/widget.r0 view only; navigation, window and result belong to the actual Record Fragment. */
class ClosedTabsPage(private val activity: ThemedActivity, applyInsets: Boolean, private val callbacks: Callbacks) : AutoCloseable {
    interface Callbacks {
        fun query(value: String)
        fun open(entry: ClosedTab)
        fun context(anchor: View, entry: ClosedTab)
        fun clear()
        fun settings()
        fun close()
    }
    private val scaffold = PageScaffold(activity, activity.getString(R.string.ref_closed_tabs_title), callbacks::close, applyInsets)
    private val adapter = ClosedTabsAdapter(activity, callbacks::open, callbacks::context)
    private val dates = LibraryDateDecoration(activity, milliseconds = true)
    val list = LibraryRecyclerView(activity).apply { setHasFixedSize(true); this.adapter = this@ClosedTabsPage.adapter; addItemDecoration(dates) }
    private val empty = NativeTypography.text(activity).apply {
        text = "¯\\_(ツ)_/¯"; textSize = 22f; gravity = Gravity.CENTER
        contentDescription = activity.getString(R.string.ref_closed_tabs_empty_description)
        setTextColor(activity.palette.secondary); setPadding(0, dp(20), 0, dp(20)); visibility = View.GONE
    }
    val editor = NativeTypography.edit(activity).apply {
        setHint(R.string.ref_closed_tabs_search)
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) { callbacks.query(value.toString()) }
            override fun afterTextChanged(value: Editable?) = Unit
        })
    }
    private val search = NativeLibrarySearch(editor)
    private val clear = button(R.string.ref_closed_tabs_clear, callbacks::clear)
    val root get() = scaffold.root
    init {
        scaffold.showContent(LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            addView(editor, LinearLayout.LayoutParams(-1, -2).apply { setMargins(activity.dp(14), activity.dp(10), activity.dp(14), activity.dp(10)) })
            addView(FrameLayout(activity).apply { addView(list, FrameLayout.LayoutParams(-1, -1)); addView(empty, FrameLayout.LayoutParams(-1, -1)) }, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(DividerLayout(activity, atBottom = false).apply {
                gravity = Gravity.CENTER_VERTICAL; addView(button(R.string.ref_closed_tabs_more, callbacks::settings))
                addView(View(activity), LinearLayout.LayoutParams(0, activity.dp(48), 1f)); addView(clear)
            }, LinearLayout.LayoutParams(-1, -2))
        })
        NativeToolbarTap.bind(scaffold.toolbar) { list.smoothScrollToPosition(0) }
    }
    fun render(records: List<ClosedTab>, removed: String? = null) {
        dates.submit(records.map { LibraryEntry(it.id, LibraryKind.HISTORY, it.tab.title, it.tab.url, updatedAt = it.closedAt) })
        adapter.submit(records, removed); list.invalidateItemDecorations()
        empty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE; list.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
        clear.isEnabled = records.isNotEmpty()
    }
    private fun button(resource: Int, action: () -> Unit) = NativeTypography.text(activity).apply {
        setText(resource); textSize = 14f; gravity = Gravity.CENTER
        setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf((activity.palette.text and 0xFFFFFF) or 0x80000000.toInt(), activity.palette.text)))
        setPadding(dp(16), 0, dp(16), 0); minimumHeight = dp(48); background = activity.touchFeedback(18); setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-2, -2)
    }
    override fun close() { search.close(); list.dispose(); list.adapter = null; adapter.close() }
}
