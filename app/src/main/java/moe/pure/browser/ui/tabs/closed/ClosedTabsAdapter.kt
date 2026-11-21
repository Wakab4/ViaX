package moe.pure.browser.ui.tabs.closed

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.browser.ClosedTab
import moe.pure.browser.feature.library.data.NativeFaviconCache
import moe.pure.browser.feature.library.ui.LibraryDisplayText
import moe.pure.browser.feature.library.ui.recycler.LibraryItemView
import moe.pure.browser.feature.library.ui.recycler.bindNativeItemClick
import moe.pure.browser.feature.settings.skin.NativeSkinIcons
import moe.pure.browser.ui.theme.palette

/** pc.a / b6.c.c: 20dp icon beside two fading text lines, with no selection checkbox. */
internal class ClosedTabsAdapter(
    private val context: Context,
    private val clicked: (ClosedTab) -> Unit,
    private val contextMenu: (View, ClosedTab) -> Unit
) : RecyclerView.Adapter<ClosedTabsAdapter.Holder>(), AutoCloseable {
    class Holder(val row: LibraryItemView) : RecyclerView.ViewHolder(row)
    var rows: List<ClosedTab> = emptyList()
        private set
    private var closed = false
    private val skin = NativeSkinIcons(context)

    fun submit(value: List<ClosedTab>, removed: String? = null) {
        val old = rows
        val index = removed?.let { id -> old.indexOfFirst { it.id == id } } ?: -1
        rows = value
        if (index >= 0 && old.size == value.size + 1 && old.filterIndexed { at, _ -> at != index }.map { it.id } == value.map { it.id })
            notifyItemRemoved(index)
        else notifyDataSetChanged()
    }
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LibraryItemView(context, false, history = true).apply {
        check.visibility = View.GONE
        subtitle.ellipsize = null; subtitle.isHorizontalFadingEdgeEnabled = true
    })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = rows[position]
        holder.row.bindNativeItemClick { rows.getOrNull(holder.bindingAdapterPosition)?.let(clicked) }
        holder.row.setOnLongClickListener {
            rows.getOrNull(holder.bindingAdapterPosition)?.let { contextMenu(holder.row, it) }
            true
        }
        holder.row.title.text = LibraryDisplayText.title(entry.tab.title).ifEmpty { context.getString(R.string.ref_closed_tabs_untitled) }
        holder.row.subtitle.text = LibraryDisplayText.url(entry.tab.url)
        icon(holder, entry)
    }
    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position) else rows.getOrNull(position)?.let { icon(holder, it) }
    }
    private fun icon(holder: Holder, entry: ClosedTab) {
        val bitmap = NativeFaviconCache.bitmap(context, entry.tab.url)
        if (bitmap != null) { holder.row.icon.imageTintList = null; holder.row.icon.setImageBitmap(bitmap) }
        else {
            holder.row.icon.imageTintList = ColorStateList.valueOf(context.palette.icon)
            holder.row.icon.setImageDrawable((skin.resolve(R.drawable.ref_ic_history) ?: context.getDrawable(R.drawable.ref_ic_history))?.mutate())
        }
    }
    override fun close() { closed = true; rows = emptyList() }
}
