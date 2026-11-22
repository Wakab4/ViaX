package moe.via.browser.feature.ai.ui.chat

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.feature.ai.model.AiThread
import moe.via.browser.ui.components.recycler.bindNativeItemClick

internal class AiThreadAdapter(private val context: Context, private val clicked: (Int) -> Unit, private val longClick: (Int) -> Boolean) : RecyclerView.Adapter<AiThreadAdapter.Holder>() {
    class Holder(val row: AiThreadRow) : RecyclerView.ViewHolder(row)
    var threads: List<AiThread> = emptyList()
        private set
    private var selected: Set<Int> = emptySet()
    fun submit(values: List<AiThread>, selection: Set<Int>) {
        if (threads == values && selected == selection) return
        val before = threads; val oldSelection = selected
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = before.size
            override fun getNewListSize() = values.size
            override fun areItemsTheSame(old: Int, new: Int) = before[old].id == values[new].id
            private fun sameValue(old: Int, new: Int) = before[old].name == values[new].name && before[old].preview == values[new].preview && before[old].updatedAt == values[new].updatedAt
            override fun areContentsTheSame(old: Int, new: Int) = sameValue(old, new) && (old in oldSelection) == (new in selection)
            override fun getChangePayload(old: Int, new: Int): Any = if (sameValue(old, new)) new in selection else Any()
        })
        threads = values; selected = selection.toSet(); diff.dispatchUpdatesTo(this)
    }
    override fun getItemCount() = threads.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(AiThreadRow(context).apply { layoutParams = RecyclerView.LayoutParams(-1, -2) })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.row.bind(threads[position], position in selected)
        holder.row.bindNativeItemClick { val index = holder.bindingAdapterPosition; if (index != RecyclerView.NO_POSITION) clicked(index) }
        holder.row.setOnLongClickListener { val index = holder.bindingAdapterPosition; index != RecyclerView.NO_POSITION && longClick(index) }
    }
    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        val selection = payloads.filterIsInstance<Boolean>()
        if (selection.isNotEmpty()) selection.forEach(holder.row::select) else onBindViewHolder(holder, position)
    }
}
