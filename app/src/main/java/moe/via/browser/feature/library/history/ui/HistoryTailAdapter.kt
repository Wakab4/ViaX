package moe.via.browser.feature.library.history.ui

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.feature.library.ui.LibraryAdapter

/** e6.a wraps the record adapter, forwarding observers so record positions never include the tail. */
internal class HistoryTailAdapter(private val records: LibraryAdapter) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private class Tail(val view: HistoryTailView) : RecyclerView.ViewHolder(view)
    var state = HistoryTailState.READY
        set(value) {
            if (field == value) return
            val had = field != HistoryTailState.END; val has = value != HistoryTailState.END
            field = value
            when { had == has -> notifyItemChanged(records.itemCount); had -> notifyItemRemoved(records.itemCount); else -> notifyItemInserted(records.itemCount) }
        }
    override fun getItemCount() = records.itemCount + if (state != HistoryTailState.END) 1 else 0
    override fun getItemViewType(position: Int) = if (position == records.itemCount) Int.MAX_VALUE else records.getItemViewType(position)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == Int.MAX_VALUE) Tail(HistoryTailView(parent.context)) else records.onCreateViewHolder(parent, viewType)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is Tail) holder.view.render(state) else records.onBindViewHolder(holder as LibraryAdapter.Holder, position)
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (holder is Tail) holder.view.render(state) else records.onBindViewHolder(holder as LibraryAdapter.Holder, position, payloads)
    }
    override fun registerAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) { super.registerAdapterDataObserver(observer); records.registerAdapterDataObserver(observer) }
    override fun unregisterAdapterDataObserver(observer: RecyclerView.AdapterDataObserver) { super.unregisterAdapterDataObserver(observer); records.unregisterAdapterDataObserver(observer) }
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) { if (holder is LibraryAdapter.Holder) records.onViewAttachedToWindow(holder) }
    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) { if (holder is LibraryAdapter.Holder) records.onViewDetachedFromWindow(holder) }
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) { if (holder is LibraryAdapter.Holder) records.onViewRecycled(holder) }
    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder) = if (holder is LibraryAdapter.Holder) records.onFailedToRecycleView(holder) else super.onFailedToRecycleView(holder)
}
