package moe.via.browser.browser.ui.tabs.switcher

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.ui.components.recycler.bindNativeItemClick
import moe.via.browser.ui.components.dp
import moe.via.browser.browser.ui.tabs.TabStripIcons
import moe.via.browser.ui.theme.palette

/** widget.b0 geometry and event-level notifications for f8.l0's live tab collection. */
internal class TabSwitcherAdapter(private val activity: Activity, private val actions: Actions) : RecyclerView.Adapter<TabSwitcherAdapter.Holder>(), AutoCloseable {
    interface Actions {
        fun select(id: String)
        fun close(id: String)
        fun context(anchor: View, id: String)
        fun drag(holder: RecyclerView.ViewHolder)
    }
    class Holder(val row: Row) : RecyclerView.ViewHolder(row)
    class Row(activity: Activity) : LinearLayout(activity) {
        val icon = ImageView(activity).apply {
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setPadding(dp(13), 0, dp(13), 0)
        }
        val title = NativeTypography.text(activity).apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
            setSingleLine(); gravity = Gravity.CENTER_VERTICAL
            ellipsize = null; isHorizontalFadingEdgeEnabled = true; setFadingEdgeLength(dp(24))
            textDirection = View.TEXT_DIRECTION_LOCALE
        }
        val close = ImageView(activity).apply {
            setPadding(dp(14), 0, dp(14), 0)
            setBackgroundResource(R.drawable.ref_tab_switcher_control_feedback)
        }
        init {
            orientation = HORIZONTAL
            layoutParams = RecyclerView.LayoutParams(-1, dp(48))
            setBackgroundResource(R.drawable.ref_tab_switcher_row_feedback)
            addView(icon, LayoutParams(dp(48), -1))
            addView(title, LayoutParams(0, -1, 1f))
            addView(close, LayoutParams(dp(48), -1))
        }
    }
    var items: List<TabSwitcherItem> = emptyList()
        private set
    private var stopped = false
    private val icons = TabStripIcons(activity)

    fun submit(value: List<TabSwitcherItem>): Boolean {
        if (stopped) return false
        val old = items
        val structural = old.map { it.id } != value.map { it.id }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = value.size
            override fun areItemsTheSame(before: Int, after: Int) = old[before].id == value[after].id
            override fun areContentsTheSame(before: Int, after: Int) = old[before] == value[after]
        })
        items = value
        diff.dispatchUpdatesTo(this)
        return structural
    }
    override fun getItemCount() = items.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(Row(activity))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.row.title.apply {
            text = item.title
            setTypeface(Typeface.DEFAULT, if (item.selected) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(if (item.selected) activity.palette.accent else activity.palette.text)
        }
        holder.row.close.contentDescription = activity.getString(R.string.ref_tab_switcher_close_title, item.title)
        holder.row.bindNativeItemClick { current(holder)?.let { actions.select(it.id) } }
        holder.row.close.bindNativeItemClick { current(holder)?.let { actions.close(it.id) } }
        holder.row.setOnLongClickListener { current(holder)?.let { actions.context(holder.row, it.id) }; true }
        holder.row.icon.setOnTouchListener { _, event ->
            if (!stopped && event.actionMasked == MotionEvent.ACTION_DOWN && current(holder) != null) actions.drag(holder)
            false
        }
        bindIcons(holder, item)
    }
    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position) else items.getOrNull(position)?.let { bindIcons(holder, it) }
    }
    private fun current(holder: Holder) = if (stopped) null else items.getOrNull(holder.bindingAdapterPosition)
    private fun bindIcons(holder: Holder, item: TabSwitcherItem) {
        icons.site(holder.row.icon, item.url, item.icon, activity.palette.icon, true)
        icons.control(holder.row.close, R.drawable.ref_ic_close, activity.palette.icon)
    }
    fun stop() { stopped = true }
    override fun close() { stop(); items = emptyList() }
}
