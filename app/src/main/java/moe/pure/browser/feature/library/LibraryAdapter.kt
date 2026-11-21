package moe.pure.browser.feature.library

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.library.ui.LibraryDisplayText
import moe.pure.browser.feature.library.ui.LibraryRowIcons
import moe.pure.browser.feature.library.ui.recycler.LibraryItemView
import moe.pure.browser.feature.library.ui.recycler.bindNativeItemClick

sealed interface LibraryRow {
    data class Item(val entry: LibraryEntry, val immutable: Boolean = false) : LibraryRow
}

internal class LibraryAdapter(private val context: Context, private val actions: Actions) : RecyclerView.Adapter<LibraryAdapter.Holder>(), AutoCloseable {
    interface Actions {
        fun click(position: Int)
        fun longClick(view: View, position: Int): Boolean
        fun drag(holder: RecyclerView.ViewHolder)
    }
    class Holder(val row: LibraryItemView) : RecyclerView.ViewHolder(row)
    var tab = LibraryTab.BOOKMARKS
    private var items: List<LibraryRow> = emptyList()
    var rows: List<LibraryRow>
        get() = items
        set(value) {
            val previous = items
            items = value.toList()
            if (tab == LibraryTab.BOOKMARKS && !editing) DiffUtil.calculateDiff(RowsDiff(previous, items)).dispatchUpdatesTo(this)
            else notifyDataSetChanged()
        }
    var editing = false
        set(value) { if (field != value) { field = value; notifyDataSetChanged() } }
    var draggingAllowed = false
        set(value) { if (field != value) { field = value; notifyDataSetChanged() } }
    var details = false
        set(value) { if (field != value) { field = value; notifyItemRangeChanged(0, itemCount, if (value) 1 else 0) } }
    val selected = mutableSetOf<String>()
    val historySelected = sortedSetOf<Int>()
    val selectionCount get() = if (tab == LibraryTab.HISTORY) historySelected.size else selected.size
    fun clearSelection() { selected.clear(); historySelected.clear() }
    fun isSelected(position: Int): Boolean = if (tab == LibraryTab.HISTORY) position in historySelected
        else (rows.getOrNull(position) as? LibraryRow.Item)?.entry?.key?.let { it in selected } == true
    fun selectedEntries(): List<LibraryEntry> = rows.mapIndexedNotNull { index, row -> if (isSelected(index)) (row as LibraryRow.Item).entry else null }
    private object IconsChanged
    private val icons = LibraryRowIcons(context)
    private fun iconsChanged() { if (itemCount > 0) notifyItemRangeChanged(0, itemCount, IconsChanged) }
    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = (rows[position] as LibraryRow.Item).entry.let { if (it.isFolder || it.kind == LibraryKind.BOOKMARK) 0 else if (it.kind == LibraryKind.HISTORY) 1 else 2 }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LibraryItemView(context, viewType == 0, viewType == 1)).also { holder ->
        holder.itemView.setOnLongClickListener { holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { actions.longClick(holder.itemView, it) } ?: false }
        val drag = View.OnTouchListener { _, event -> if (event.actionMasked == MotionEvent.ACTION_DOWN) actions.drag(holder); true }
        if (holder.row.bookmark) { holder.row.icon.setOnTouchListener(drag); holder.row.handle.setOnTouchListener(drag) }
    }
    override fun onBindViewHolder(holder: Holder, position: Int) = bind(holder, position)
    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) { bind(holder, position); return }
        val item = rows.getOrNull(position) as? LibraryRow.Item ?: return
        payloads.forEach { payload ->
            when (payload) {
                IconsChanged -> icons.bind(holder.row.icon, item.entry)
                is Boolean -> holder.row.check.isChecked = payload
                is Int -> bindDetails(holder.row, item)
                else -> Unit
            }
        }
    }
    private fun bind(holder: Holder, position: Int) {
        val item = rows[position] as LibraryRow.Item
        val entry = item.entry; val row = holder.row
        row.bindNativeItemClick { holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let(actions::click) }
        icons.bind(row.icon, entry)
        val title = LibraryDisplayText.title(entry.title)
        row.title.text = title
        row.title.contentDescription = if (entry.isFolder) context.getString(R.string.ref_library_folder_description, title) else null
        row.check.visibility = if (editing && !item.immutable) View.VISIBLE else View.GONE
        row.check.isChecked = isSelected(position)
        row.handle.visibility = if (row.bookmark && editing && draggingAllowed && !item.immutable) View.VISIBLE else View.GONE
        bindDetails(row, item)
    }
    private fun bindDetails(row: LibraryItemView, item: LibraryRow.Item) {
        val entry = item.entry
        row.subtitle.text = if (entry.isFolder) context.resources.getQuantityString(R.plurals.ref_library_item_count, entry.childCount, entry.childCount)
            else LibraryDisplayText.url(entry.url)
        row.subtitle.visibility = if (!item.immutable && (details || entry.kind == LibraryKind.HISTORY) && (entry.isFolder || entry.url.isNotBlank())) View.VISIBLE else View.GONE
    }
    fun select(position: Int, value: Boolean) {
        val item = rows.getOrNull(position) as? LibraryRow.Item ?: return
        if (item.immutable) return
        val changed = if (tab == LibraryTab.HISTORY) { if (value) historySelected.add(position) else historySelected.remove(position) }
            else if (value) selected.add(item.entry.key) else selected.remove(item.entry.key)
        if (changed) notifyItemChanged(position, value)
    }
    fun move(from: Int, to: Int) {
        val next = rows.toMutableList(); val moving = next.removeAt(from); next.add(to, moving)
        items = next; notifyItemMoved(from, to)
    }
    fun remove(keys: Set<String>) {
        for (position in rows.indices.reversed()) {
            if ((rows[position] as LibraryRow.Item).entry.key in keys) {
                items = items.toMutableList().apply { removeAt(position) }; notifyItemRemoved(position)
            }
        }
    }
    fun replaceBacking(value: List<LibraryRow>) { items = value }
    fun removedPositions(positions: List<Int>) { positions.forEach(::notifyItemRemoved) }
    fun refreshIcons() { iconsChanged() }
    override fun close() { clearSelection() }

    /** cb.c: identity is kind/id, whereas title/URL/count determine content. */
    private class RowsDiff(private val old: List<LibraryRow>, private val new: List<LibraryRow>) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size
        override fun areItemsTheSame(a: Int, b: Int): Boolean {
            val first = old[a] as LibraryRow.Item; val second = new[b] as LibraryRow.Item
            return first.entry.key == second.entry.key && first.immutable == second.immutable
        }
        override fun areContentsTheSame(a: Int, b: Int): Boolean {
            val first = (old[a] as LibraryRow.Item).entry; val second = (new[b] as LibraryRow.Item).entry
            return first.isFolder == second.isFolder && first.title == second.title && first.url == second.url && first.childCount == second.childCount
        }
    }
}
