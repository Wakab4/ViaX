package moe.pure.browser.feature.site.ui.redirect

import moe.pure.browser.ui.settings.NativeSettingViews

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
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

internal class RedirectSettingsAdapter(private val activity: Activity, private val clicked: (RedirectSettingRow) -> Unit) :
    RecyclerView.Adapter<RedirectSettingsAdapter.Holder>(), AutoCloseable {
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    private var rows = emptyList<RedirectSettingRow>()
    private var closed = false
    private val skin = NativeSkinIcons(activity)

    fun submit(value: List<RedirectSettingRow>) {
        if (closed) return
        val previous = rows
        val changes = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = value.size
            override fun areItemsTheSame(old: Int, new: Int): Boolean {
                val a = previous[old]; val b = value[new]
                return when {
                    a is RedirectSettingRow.Global && b is RedirectSettingRow.Global -> true
                    a === RedirectSettingRow.Add && b === RedirectSettingRow.Add -> true
                    a is RedirectSettingRow.Section && b is RedirectSettingRow.Section -> true
                    a is RedirectSettingRow.Site && b is RedirectSettingRow.Site -> a.key.hashCode() == b.key.hashCode()
                    else -> false
                }
            }
            override fun areContentsTheSame(old: Int, new: Int) = previous[old] == value[new]
            override fun getChangePayload(old: Int, new: Int): Any = Unit
        })
        rows = value; changes.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = when (rows[position]) {
        is RedirectSettingRow.Global -> 0
        RedirectSettingRow.Add -> 1
        is RedirectSettingRow.Section -> 2
        is RedirectSettingRow.Site -> 3
    }
    override fun onCreateViewHolder(parent: ViewGroup, type: Int): Holder {
        val view = when (type) {
            0 -> NativeSettingViews.Global(activity)
            1 -> NativeSettingViews.add(activity)
            2 -> NativeSettingViews.section(activity)
            else -> NativeSettingViews.site(activity)
        }
        view.layoutParams = RecyclerView.LayoutParams(-1, -2)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        when (row) {
            is RedirectSettingRow.Global -> (holder.itemView as NativeSettingViews.Global).apply {
                title.setText(R.string.ref_redirect_page_title)
                detail.setText(if (row.allowed) R.string.ref_redirect_page_global_allow else R.string.ref_redirect_page_global_ask)
                check.isChecked = row.allowed
            }
            RedirectSettingRow.Add -> (holder.itemView as TextView).apply { setText(R.string.ref_redirect_page_add); icon(this) }
            is RedirectSettingRow.Section -> (holder.itemView as TextView).text = row.title
            is RedirectSettingRow.Site -> ((holder.itemView as LinearLayout).getChildAt(0) as TextView).text = row.key
        }
        if (row is RedirectSettingRow.Section) holder.itemView.setOnClickListener(null)
        else {
            var last: Long? = null
            holder.itemView.setOnClickListener {
                val now = SystemClock.elapsedRealtime()
                if (!closed && holder.bindingAdapterPosition != RecyclerView.NO_POSITION &&
                    (last == null || kotlin.math.abs(now - last!!) > 300)) {
                    last = now; clicked(row)
                }
            }
        }
    }

    private fun icon(view: TextView) {
        val image = (skin.resolve(R.drawable.ref_ic_add) ?: activity.getDrawable(R.drawable.ref_ic_add))?.mutate()
        image?.apply { setTint(activity.palette.text); setBounds(0, 0, activity.dp(18), activity.dp(18)) }
        view.setCompoundDrawablesRelative(image, null, null, null)
    }

    override fun close() { closed = true; rows = emptyList() }
}
