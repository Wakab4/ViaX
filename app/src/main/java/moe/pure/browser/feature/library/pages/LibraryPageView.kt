package moe.pure.browser.feature.library.pages

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.feature.library.LibraryTab
import moe.pure.browser.feature.library.ui.HistoryFooterView
import moe.pure.browser.feature.library.ui.NativeLibrarySearch
import moe.pure.browser.feature.library.ui.recycler.LibraryRecyclerView
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.surface
import moe.pure.browser.ui.components.touchFeedback
import moe.pure.browser.ui.theme.palette

class LibraryPageView(private val activity: Activity, private val tab: LibraryTab, actions: Actions) : AutoCloseable {
    interface Actions {
        fun queryChanged(value: String)
        fun footerLeft()
        fun footerCenter()
        fun footerRight()
        fun footerMove()
        fun footerOpen()
    }
    val root = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(activity.palette.canvas) }
    val list = LibraryRecyclerView(activity)
    val search: EditText = NativeTypography.edit(activity).apply {
        setHint(R.string.ref_closed_tabs_search); textSize = 14f; setSingleLine(true)
        background = activity.surface(activity.palette.input, 20); setTextColor(activity.palette.text); setHintTextColor(activity.palette.hint)
        setPadding(dp(16), 0, dp(16), 0); imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { actions.queryChanged(s?.toString().orEmpty()) }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }
    private val empty = NativeTypography.text(activity).apply {
        text = "¯\\_(ツ)_/¯"; contentDescription = activity.getString(R.string.ref_closed_tabs_empty_description)
        textSize = 22f; gravity = Gravity.CENTER; setTextColor(activity.palette.secondary)
        visibility = View.GONE
    }
    private val nativeSearch = NativeLibrarySearch(search, initiallyEnabled = tab == LibraryTab.HISTORY)
    private val footer = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL }
    private val left = button(actions::footerLeft)
    private val center = button(actions::footerCenter)
    private val right = button(actions::footerRight)
    private val move = button(actions::footerMove).apply { setText(R.string.ref_library_host_move) }
    private val open = button(actions::footerOpen).apply { setText(R.string.ref_library_host_open) }
    private val historyFooter = HistoryFooterView(activity, actions::footerLeft, actions::footerCenter, actions::footerRight)
    val historyActionAnchor: View get() = historyFooter.clearAnchor
    val bookmarkMoreAnchor: View get() = left
    init {
        root.addView(search, LinearLayout.LayoutParams(-1, activity.dp(40)).apply { setMargins(activity.dp(14), activity.dp(10), activity.dp(14), activity.dp(10)) })
        root.addView(FrameLayout(activity).apply { addView(list, FrameLayout.LayoutParams(-1, -1)); addView(empty, FrameLayout.LayoutParams(-1, -1)) }, LinearLayout.LayoutParams(-1, 0, 1f))
        if (tab == LibraryTab.HISTORY) root.addView(historyFooter, LinearLayout.LayoutParams(-1, -2))
        else {
            root.addView(View(activity).apply { setBackgroundColor(activity.palette.divider) }, LinearLayout.LayoutParams(-1, 1))
            footer.addView(left, LinearLayout.LayoutParams(-2, -1)); footer.addView(View(activity), LinearLayout.LayoutParams(0, 1, 1f))
            listOf(center, move, open, right).forEach { footer.addView(it, LinearLayout.LayoutParams(-2, -1)) }
            root.addView(footer, LinearLayout.LayoutParams(-1, activity.dp(48)))
        }
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0 && search.hasFocus()) { search.clearFocus(); activity.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(search.windowToken, 0) }
            }
        })
    }
    fun historyRows(empty: Boolean, queryEmpty: Boolean) = historyFooter.rows(empty, queryEmpty)
    fun update(editing: Boolean, hasItems: Boolean, selected: Int, historyEmpty: Boolean = false, historyCount: Int = 0) {
        val noRows = if (tab == LibraryTab.HISTORY) historyEmpty else list.adapter?.itemCount == 0
        list.visibility = if (noRows) View.GONE else View.VISIBLE; empty.visibility = if (noRows) View.VISIBLE else View.GONE
        if (tab == LibraryTab.HISTORY) { historyFooter.render(editing, selected, historyCount); return }
        left.text = if (editing) activity.getString(R.string.ref_history_footer_select_all) else if (tab == LibraryTab.BOOKMARKS) activity.getString(R.string.ref_bookmark_more_title) else ""
        left.visibility = if (left.text.isEmpty()) View.GONE else View.VISIBLE
        center.text = if (editing) activity.getString(R.string.ref_library_context_delete) else ""
        center.visibility = if (center.text.isEmpty()) View.GONE else View.VISIBLE; center.isEnabled = selected > 0
        right.setText(if (editing) R.string.ref_bookmark_form_done else R.string.ref_library_dialog_edit); right.isEnabled = editing || hasItems
        move.visibility = if (editing && tab == LibraryTab.BOOKMARKS) View.VISIBLE else View.GONE; open.visibility = if (editing) View.VISIBLE else View.GONE
        move.isEnabled = selected > 0; open.isEnabled = selected > 0; move.alpha = if (selected > 0) 1f else .35f; open.alpha = move.alpha
        center.setTextColor(if (center.isEnabled) activity.palette.text else 0xFF999999.toInt()); right.setTextColor(if (right.isEnabled) activity.palette.text else 0xFF999999.toInt())
    }
    private fun button(action: () -> Unit) = NativeTypography.text(activity).apply {
        textSize = 14f; gravity = Gravity.CENTER; setTextColor(activity.palette.text); setPadding(dp(16), 0, dp(16), 0)
        background = activity.touchFeedback(); setOnClickListener { action() }
    }
    override fun close() { nativeSearch.close(); list.dispose(); list.adapter = null }
}
