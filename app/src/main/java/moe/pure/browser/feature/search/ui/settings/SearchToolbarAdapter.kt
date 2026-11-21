package moe.pure.browser.feature.search.ui.settings

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.search.model.SearchEngine
import moe.pure.browser.ui.settings.NativeSettingViews
import kotlin.math.abs

internal class SearchToolbarAdapter(private val context: Context, private val toggle: () -> Unit,
    private val click: (Int) -> Unit, private val range: (Int) -> Unit,
    private val drag: (RecyclerView.ViewHolder) -> Unit) : RecyclerView.Adapter<SearchToolbarAdapter.Holder>() {
    data class Item(val engine: SearchEngine, val default: Boolean, var checked: Boolean)
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    val entries = ArrayList<Item>()
    private var enabled = false
    override fun getItemCount() = entries.size + 2
    override fun getItemViewType(position: Int) = if (position < 2) position else 2
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(when (viewType) {
        0 -> NativeSettingViews.Global(context)
        1 -> NativeSettingViews.section(context)
        else -> SearchToolbarRow(context)
    }.apply { layoutParams = RecyclerView.LayoutParams(-1, -2) })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        when (position) {
            0 -> (holder.itemView as NativeSettingViews.Global).apply {
                title.setText(R.string.ref_search_page_enable_toolbar); detail.visibility = View.GONE; check.isChecked = enabled
                clicked(this, toggle)
            }
            1 -> (holder.itemView as TextView).setText(R.string.ref_search_page_engine_section)
            else -> (holder.itemView as SearchToolbarRow).apply {
                val item = entries[position - 2]
                title.text = item.engine.name; detail.text = if (item.default) context.getString(R.string.ref_search_page_default) else ""
                detail.visibility = if (item.default) View.VISIBLE else View.GONE; check.isChecked = item.checked
                listOf(title, detail, check, handle).forEach { it.alpha = if (enabled) 1f else .5f }
                isEnabled = enabled
                clicked(this) { val at = holder.bindingAdapterPosition; if (at >= 2) click(at) }
                setOnLongClickListener { val at = holder.bindingAdapterPosition; if (at >= 2) range(at); true }
                handle.setOnTouchListener { view, event -> if (event.action == MotionEvent.ACTION_DOWN && view.alpha == 1f) drag(holder); true }
            }
        }
    }
    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (position >= 2 && payloads.isNotEmpty() && payloads.all { it is Boolean })
            (holder.itemView as SearchToolbarRow).check.isChecked = payloads.last() as Boolean
        else super.onBindViewHolder(holder, position, payloads)
    }
    private fun clicked(view: View, action: () -> Unit) {
        var last: Long? = null
        view.setOnClickListener { val now = SystemClock.elapsedRealtime(); if (last == null || abs(now - last!!) > 300) { last = now; action() } }
    }
    fun submit(next: List<Item>, active: Boolean) {
        val before = entries.toList(); val previouslyEnabled = enabled
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = before.size + 2
            override fun getNewListSize() = next.size + 2
            override fun areItemsTheSame(old: Int, new: Int) = if (old < 2 || new < 2) old == new else before[old - 2].engine.id == next[new - 2].engine.id
            override fun areContentsTheSame(old: Int, new: Int) = when { old == 0 -> previouslyEnabled == active; old == 1 -> true; else -> previouslyEnabled == active && before[old - 2] == next[new - 2] }
        })
        entries.clear(); entries.addAll(next); enabled = active; diff.dispatchUpdatesTo(this)
    }
    fun select(position: Int, selected: Boolean) {
        val item = entries[position - 2]
        if (item.default || item.checked == selected) return
        item.checked = selected; notifyItemChanged(position, selected)
    }
    fun moved(from: Int, to: Int) { entries.add(to - 2, entries.removeAt(from - 2)) }
}
