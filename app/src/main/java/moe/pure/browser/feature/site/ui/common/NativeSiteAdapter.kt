package moe.pure.browser.feature.site.ui.common

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.settings.skin.NativeSkinIcons
import moe.pure.browser.feature.site.ui.font.NativeFontRowView
import moe.pure.browser.ui.settings.NativeSettingViews
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** a6/ib presentation only. Feature owners supply all decisions and persistence. */
class NativeSiteAdapter(private val activity: Activity, private val onRowClick: (NativeSiteRow) -> Unit) :
    RecyclerView.Adapter<NativeSiteAdapter.Holder>(), AutoCloseable {
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    var onSliderValue: ((Int, Int) -> Unit)? = null
    private var rows = emptyList<NativeSiteRow>()
    private var closed = false
    private val skin = NativeSkinIcons(activity)

    fun submit(value: List<NativeSiteRow>) {
        if (closed) return
        val previous = rows
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = value.size
            override fun areItemsTheSame(old: Int, new: Int): Boolean {
                val a = previous[old]; val b = value[new]
                return a.javaClass == b.javaClass && (a is NativeSiteRow.Section || a.id == b.id)
            }
            override fun areContentsTheSame(old: Int, new: Int) =
                previous[old] is NativeSiteRow.Slider && value[new] is NativeSiteRow.Slider || previous[old] == value[new]
            override fun getChangePayload(old: Int, new: Int): Any = Unit
        })
        rows = value; diff.dispatchUpdatesTo(this)
    }
    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = when (rows[position]) {
        is NativeSiteRow.Toggle -> 0
        is NativeSiteRow.Link -> 1
        is NativeSiteRow.Section -> 2
        is NativeSiteRow.Add -> 3
        is NativeSiteRow.Slider -> 4
    }
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): Holder = Holder(when (type) {
        0 -> NativeSettingViews.Global(activity)
        1 -> NativeSettingViews.site(activity)
        2 -> NativeSettingViews.section(activity)
        3 -> NativeSettingViews.add(activity)
        else -> NativeFontRowView(activity)
    }.apply { layoutParams = RecyclerView.LayoutParams(-1, -2) })

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        var disabled = false
        when (row) {
            is NativeSiteRow.Toggle -> (holder.itemView as NativeSettingViews.Global).apply {
                title.text = row.title; detail.text = row.detail
                detail.visibility = if (row.detail.isNullOrEmpty()) View.GONE else View.VISIBLE
                check.isChecked = row.checked
                val alpha = if (row.disabled) .5f else 1f
                title.alpha = alpha; detail.alpha = alpha; check.alpha = alpha
                disabled = row.disabled
            }
            is NativeSiteRow.Link -> (holder.itemView as LinearLayout).apply {
                (getChildAt(0) as TextView).text = row.title
                (getChildAt(1) as TextView).apply {
                    text = row.detail; visibility = if (row.detail.isNullOrEmpty()) View.GONE else View.VISIBLE
                }
                val alpha = if (row.disabled) .5f else 1f
                getChildAt(0).alpha = alpha; getChildAt(1).alpha = alpha; disabled = row.disabled
            }
            is NativeSiteRow.Section -> (holder.itemView as TextView).text = row.title
            is NativeSiteRow.Add -> (holder.itemView as TextView).apply { text = row.title; bindIcon(this) }
            is NativeSiteRow.Slider -> (holder.itemView as NativeFontRowView).bind(row) { onSliderValue?.invoke(row.id, it) }
        }
        holder.itemView.isEnabled = !disabled
        if (row is NativeSiteRow.Section || row is NativeSiteRow.Slider) holder.itemView.setOnClickListener(null)
        else {
            var last: Long? = null
            holder.itemView.setOnClickListener {
                val now = SystemClock.elapsedRealtime()
                if (!closed && it.isEnabled && holder.bindingAdapterPosition != RecyclerView.NO_POSITION &&
                    (last == null || kotlin.math.abs(now - last!!) > 300)) {
                    last = now; onRowClick(row)
                }
            }
        }
    }
    private fun bindIcon(view: TextView) {
        val drawable = (skin.resolve(R.drawable.ref_ic_add) ?: activity.getDrawable(R.drawable.ref_ic_add))?.mutate()
        drawable?.apply { setTint(activity.palette.text); setBounds(0, 0, activity.dp(18), activity.dp(18)) }
        view.setCompoundDrawablesRelative(drawable, null, null, null)
    }
    override fun close() { closed = true; rows = emptyList(); onSliderValue = null }
}
