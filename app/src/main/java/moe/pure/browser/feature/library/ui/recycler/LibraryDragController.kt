package moe.pure.browser.feature.library.ui.recycler

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.feature.library.LibraryAdapter
import moe.pure.browser.feature.library.LibraryRow
import moe.pure.browser.ui.components.touchFeedback
import moe.pure.browser.ui.theme.palette

/** cb.e0.e uses ItemTouchHelper's actual drag/settle engine, with separate folder/bookmark regions. */
internal class LibraryDragController(private val list: RecyclerView, private val adapter: LibraryAdapter, private val actions: Actions) : AutoCloseable {
    interface Actions {
        fun canStart(): Boolean
        fun enabled(): Boolean
        fun persist(folder: Boolean)
    }
    private var changed = false
    private var folder = false
    private val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun isLongPressDragEnabled() = false
        override fun isItemViewSwipeEnabled() = false
        override fun onMove(rv: RecyclerView, source: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            if (!actions.enabled()) return false
            val from = source.bindingAdapterPosition; val to = target.bindingAdapterPosition
            val moving = adapter.rows.getOrNull(from) as? LibraryRow.Item ?: return false
            val destination = adapter.rows.getOrNull(to) as? LibraryRow.Item ?: return false
            if (moving.immutable || destination.immutable || moving.entry.isFolder != destination.entry.isFolder) return false
            adapter.move(from, to); changed = true; folder = moving.entry.isFolder
            return true
        }
        override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = Unit
        override fun onSelectedChanged(holder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(holder, actionState)
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) holder?.itemView?.setBackgroundColor(0x30808080)
            if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && changed) { changed = false; actions.persist(folder) }
        }
        override fun clearView(rv: RecyclerView, holder: RecyclerView.ViewHolder) {
            super.clearView(rv, holder); holder.itemView.background = rv.context.touchFeedback()
        }
    })
    init { helper.attachToRecyclerView(list) }
    fun start(holder: RecyclerView.ViewHolder) {
        if (adapter.rows.getOrNull(holder.bindingAdapterPosition) !is LibraryRow.Item) return
        // Native allows the icon to lift even an immutable/name-sorted row; onMove rejects its relocation.
        if (actions.canStart()) helper.startDrag(holder)
    }
    override fun close() { helper.attachToRecyclerView(null) }
}
