package moe.pure.browser.feature.adblock.ui

import android.app.Activity
import android.os.SystemClock
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.feature.adblock.FilterSubscription
import moe.pure.browser.feature.settings.SettingRow
import moe.pure.browser.feature.settings.SettingsRenderer
import moe.pure.browser.ui.settings.NativeSettingViews

/** ib.f compares subscription identity by URL, but always rebinds its mutable metadata. */
internal class FilterSubscriptionRows(activity: Activity, private val interval: () -> Unit,
    private val toggle: (FilterSubscription) -> Unit, private val menu: (FilterSubscription, View) -> Unit,
    private val detail: (FilterSubscription) -> String) : RecyclerView.Adapter<FilterSubscriptionRows.Holder>() {
    private sealed interface Row {
        data class Automatic(val title: String, val value: String) : Row
        data class Section(val title: String) : Row
        data class Subscription(val item: FilterSubscription) : Row
    }
    class Holder(view: View) : RecyclerView.ViewHolder(view) { var lastTap: Long? = null }
    private val renderer = SettingsRenderer(activity)
    private var rows: List<Row> = emptyList()
    fun submit(items: List<FilterSubscription>, automatic: String, value: String, section: String) {
        val before = rows
        val next = buildList {
            if (items.isNotEmpty()) { add(Row.Automatic(automatic, value)); add(Row.Section(section)) }
            items.forEach { add(Row.Subscription(it)) }
        }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = before.size
            override fun getNewListSize() = next.size
            override fun areItemsTheSame(old: Int, new: Int): Boolean {
                val a = before[old]; val b = next[new]
                return if (a is Row.Subscription && b is Row.Subscription) a.item.url == b.item.url else a.javaClass == b.javaClass
            }
            override fun areContentsTheSame(old: Int, new: Int) = before[old] !is Row.Subscription && before[old] == next[new]
        })
        rows = next; diff.dispatchUpdatesTo(this)
    }
    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = when (rows[position]) { is Row.Automatic -> 0; is Row.Section -> 3; is Row.Subscription -> 1 }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(renderer.empty(viewType).apply {
        layoutParams = RecyclerView.LayoutParams(-1, -2)
    })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        when (val row = rows[position]) {
            is Row.Automatic -> renderer.bind(holder.itemView, SettingRow.Link(row.title, row.value) {})
            is Row.Section -> renderer.bind(holder.itemView, SettingRow.Section(row.title))
            is Row.Subscription -> (holder.itemView as NativeSettingViews.Global).apply {
                title.text = row.item.title?.takeIf { it.isNotEmpty() } ?: row.item.url.substringAfter("://", row.item.url)
                title.ellipsize = TextUtils.TruncateAt.MIDDLE
                detail.text = this@FilterSubscriptionRows.detail(row.item); detail.visibility = View.VISIBLE; check.isChecked = row.item.enabled
            }
        }
        holder.lastTap = null
        holder.itemView.setOnClickListener {
            val index = holder.bindingAdapterPosition
            if (index != RecyclerView.NO_POSITION) {
                val now = SystemClock.elapsedRealtime()
                if (holder.lastTap == null || kotlin.math.abs(now - holder.lastTap!!) > 300) {
                    holder.lastTap = now
                    when (val row = rows[index]) {
                        is Row.Automatic -> interval()
                        is Row.Subscription -> { toggle(row.item); notifyItemChanged(index) }
                        is Row.Section -> Unit
                    }
                }
            }
        }
        holder.itemView.setOnLongClickListener {
            val index = holder.bindingAdapterPosition
            if (index != RecyclerView.NO_POSITION) (rows[index] as? Row.Subscription)?.let { menu(it.item, holder.itemView) }
            false
        }
    }
}
