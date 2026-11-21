package moe.pure.browser.feature.fonts.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.FontEntry
import moe.pure.browser.feature.fonts.FontPreviewCache
import moe.pure.browser.feature.settings.SettingRow
import moe.pure.browser.feature.settings.SettingsRenderer

/** hb.j2/a6.g binds visible previews and diffs entry identity, name and checked state. */
internal class FontListAdapter(private val activity: Activity, private val previews: FontPreviewCache,
    private val selected: (FontEntry) -> Unit, private val menu: (FontEntry, View) -> Unit) : RecyclerView.Adapter<FontListAdapter.Holder>() {
    private data class Row(val entry: FontEntry, val id: Int, val title: String, val checked: Boolean)
    private var rows: List<Row> = emptyList()
    private val renderer = SettingsRenderer(activity)
    class Holder(val root: FrameLayout) : RecyclerView.ViewHolder(root)

    fun submit(entries: List<FontEntry>, selected: String) {
        val old = rows
        val next = entries.map { Row(it, it.hashCode(), it.name.ifEmpty { activity.getString(R.string.ref_font_default) }, it.name == selected) }
        val changes = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = next.size
            override fun areItemsTheSame(oldPosition: Int, newPosition: Int) = old[oldPosition].id == next[newPosition].id
            override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
                val before = old[oldPosition]; val after = next[newPosition]
                return before.id == after.id && before.title == after.title && before.checked == after.checked
            }
            override fun getChangePayload(oldPosition: Int, newPosition: Int): Any = Any()
        })
        rows = next
        changes.dispatchUpdatesTo(this)
    }
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(FrameLayout(activity).apply {
        layoutParams = RecyclerView.LayoutParams(-1, -2)
    })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        val face = previews.typeface(row.entry)
        val view = renderer.create(SettingRow.Radio(row.title, row.checked, typeface = face, dimmed = previews.invalid(row.entry)) {
            val current = holder.bindingAdapterPosition
            if (current != RecyclerView.NO_POSITION) selected(rows[current].entry)
        })
        view.setOnLongClickListener {
            val current = holder.bindingAdapterPosition
            if (current != RecyclerView.NO_POSITION) menu(rows[current].entry, it)
            true
        }
        holder.root.removeAllViews()
        holder.root.addView(view, FrameLayout.LayoutParams(-1, -2))
    }
}
