package moe.via.browser.feature.settings.ui

import android.app.Activity
import android.graphics.Typeface
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.ColorUtils
import moe.via.browser.feature.settings.SettingRow
import moe.via.browser.feature.settings.SettingsItem
import moe.via.browser.feature.settings.SettingsRenderer
import moe.via.browser.feature.settings.ToolbarPreviewState
import moe.via.browser.ui.components.surface
import moe.via.browser.ui.theme.palette

/** Standard settings keep one recycled widget per row kind; callbacks resolve the live position. */
internal class SettingsRowAdapter(activity: Activity, private val changed: () -> Unit) : RecyclerView.Adapter<SettingsRowAdapter.Holder>() {
    private data class Visual(val title: String, val detail: String = "", val checked: Boolean = false,
        val enabled: Boolean = true, val dimmed: Boolean = false, val highlighted: Boolean = false,
        val typeface: Typeface? = null, val model: Any? = null)
    private data class Item(val source: SettingsItem, val kind: Int, val visual: Visual) {
        val row get() = source.row
        fun sameIdentity(next: Item): Boolean {
            if (kind != next.kind) return false
            return when (source) {
                is SettingsItem.Entry -> next.source is SettingsItem.Entry && source.id == next.source.id
                is SettingsItem.Section -> next.source is SettingsItem.Section
                is SettingsItem.Content -> next.source is SettingsItem.Content && source.key == next.source.key
            }
        }
    }
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    private val renderer = SettingsRenderer(activity)
    private var rows: List<Item> = emptyList()
    var searchTarget: Int? = null

    fun submit(value: List<SettingsItem>) {
        val before = rows
        val next = value.map { source ->
            val row = source.row
            val visual = when (row) {
                is SettingRow.Link -> Visual(row.title, row.detail, enabled = row.enabled, highlighted = row.highlighted)
                is SettingRow.Toggle -> Visual(row.title, row.detail, row.checked, row.enabled)
                is SettingRow.Radio -> Visual(row.title, row.detail, checked = row.checked, enabled = row.enabled, dimmed = row.dimmed, typeface = row.typeface)
                is SettingRow.Section -> Visual(row.title)
                is SettingRow.Custom -> Visual("", model = (source as SettingsItem.Content).model)
            }
            Item(source, renderer.kind(row), visual)
        }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = before.size
            override fun getNewListSize() = next.size
            override fun areItemsTheSame(old: Int, new: Int) = before[old].sameIdentity(next[new])
            override fun areContentsTheSame(old: Int, new: Int) = before[old].visual == next[new].visual
            override fun getChangePayload(old: Int, new: Int): Any? {
                val previous = before[old].visual.model
                val current = next[new].visual.model
                if (previous is ToolbarPreviewState && current is ToolbarPreviewState) {
                    val changes = previous.changes(current)
                    return if (changes == 7) null else changes
                }
                return Any()
            }
        })
        rows = next
        diff.dispatchUpdatesTo(this)
    }
    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = rows[position].kind
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(renderer.empty(viewType).apply {
        layoutParams = RecyclerView.LayoutParams(-1, -2)
    })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = rows[position]
        val row = item.row
        renderer.bind(holder.itemView, row)
        searchTarget?.let { target ->
            val view = holder.itemView
            view.foreground = if ((item.source as? SettingsItem.Entry)?.id == target) {
                view.context.surface(ColorUtils.setAlphaComponent(view.context.palette.accent, 30), 12)
            } else null
        }
        if (row is SettingRow.Section || row is SettingRow.Custom) {
            holder.itemView.setOnClickListener(null); holder.itemView.setOnLongClickListener(null)
            return
        }
        var last: Long? = null
        holder.itemView.setOnClickListener {
            val current = holder.bindingAdapterPosition
            val now = SystemClock.elapsedRealtime()
            if (current != RecyclerView.NO_POSITION && (last == null || kotlin.math.abs(now - last!!) > 300)) {
                last = now
                val value = rows[current].row
                renderer.activate(it, value)
                if (value is SettingRow.Toggle || value is SettingRow.Radio) changed()
            }
        }
        holder.itemView.setOnLongClickListener {
            val current = holder.bindingAdapterPosition
            current != RecyclerView.NO_POSITION && renderer.longClick(rows[current].row)
        }
    }
    fun positionOf(entry: Int) = rows.indexOfFirst { (it.source as? SettingsItem.Entry)?.id == entry }
    fun selectedPosition() = rows.indexOfFirst { it.row is SettingRow.Radio && it.visual.checked }
}
