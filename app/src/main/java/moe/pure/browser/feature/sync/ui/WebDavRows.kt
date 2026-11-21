package moe.pure.browser.feature.sync.ui

import android.app.Activity
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.settings.SettingRow
import moe.pure.browser.feature.settings.SettingsRenderer

internal class WebDavRows(private val activity: Activity) : RecyclerView.Adapter<WebDavRows.Holder>() {
    data class Account(val title: String, val detail: String, val configured: Boolean,
        val edit: () -> Unit, val sync: () -> Unit, val manual: () -> Unit)
    private data class Row(val id: Int, val kind: Int, val value: Any, val visual: Any)
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    private val renderer = SettingsRenderer(activity)
    private var rows: List<Row> = emptyList()
    fun submit(account: Account, section: String, automatic: SettingRow.Toggle, data: SettingRow.Link, manual: SettingRow.Link) {
        val next = listOf(
            Row(2, 5, account, Triple(account.title, account.detail, account.configured)),
            Row(0, 3, SettingRow.Section(section), section),
            Row(3, 1, automatic, listOf(automatic.title, automatic.checked, automatic.enabled)),
            Row(4, 0, data, listOf(data.title, data.detail, data.enabled)),
            Row(5, 0, manual, listOf(manual.title, manual.enabled))
        )
        val before = rows
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = before.size
            override fun getNewListSize() = next.size
            override fun areItemsTheSame(old: Int, new: Int) = before[old].id == next[new].id && before[old].kind == next[new].kind
            override fun areContentsTheSame(old: Int, new: Int) = before[old].visual == next[new].visual
        })
        rows = next; diff.dispatchUpdatesTo(this)
    }
    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = rows[position].kind
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        (if (viewType == 5) WebDavAccountRow(activity) else renderer.empty(viewType)).apply { layoutParams = RecyclerView.LayoutParams(-1, -2) }
    )
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        if (row.value is Account) {
            val account = row.value
            (holder.itemView as WebDavAccountRow).apply {
                title.text = account.title; detail.text = account.detail
                refresh.contentDescription = activity.getString(R.string.ref_sync_page_title)
                refresh.isEnabled = account.configured; refresh.alpha = if (account.configured) 1f else .5f
                click(this, account.edit); click(refresh, account.sync)
                refresh.setOnLongClickListener { account.manual(); true }
            }
        } else {
            renderer.bind(holder.itemView, row.value as SettingRow)
            click(holder.itemView) {
                val index = holder.bindingAdapterPosition
                if (index != RecyclerView.NO_POSITION) renderer.activate(holder.itemView, rows[index].value as SettingRow)
            }
        }
    }
    private fun click(view: View, action: () -> Unit) {
        var last: Long? = null
        view.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            if (last == null || kotlin.math.abs(now - last!!) > 300) { last = now; action() }
        }
    }
}
