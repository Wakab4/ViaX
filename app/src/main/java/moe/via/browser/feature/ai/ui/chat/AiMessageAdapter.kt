package moe.via.browser.feature.ai.ui.chat

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.feature.ai.model.AiMessageDisplay
import moe.via.browser.feature.ai.model.AiUserMessageDisplay

internal class AiMessageAdapter(private val context: Context,
    private val action: (AiMessageDisplay, Int, View) -> Unit) : RecyclerView.Adapter<AiMessageAdapter.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    private var messages: List<AiMessageDisplay> = emptyList()
    var generation = 1
        private set
    fun submit(values: List<AiMessageDisplay>, list: RecyclerView) {
        val before = messages
        var reset = values.isEmpty() == before.isNotEmpty()
        if (!reset && values.isNotEmpty() && before.isNotEmpty()) {
            val sameFirst = values[0].id!!.equals(before[0].id)
            if (sameFirst) generation++
            reset = !sameFirst
        }
        if (reset) { messages = values; notifyDataSetChanged() } else {
            val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize() = before.size
                override fun getNewListSize() = values.size
                override fun areItemsTheSame(old: Int, new: Int) = before[old].javaClass == values[new].javaClass && before[old].id == values[new].id
                override fun areContentsTheSame(old: Int, new: Int) = before[old].javaClass == values[new].javaClass && before[old].text == values[new].text
                override fun getChangePayload(old: Int, new: Int): Any? = if (before[old].javaClass == values[new].javaClass) Any() else null
            })
            messages = values; diff.dispatchUpdatesTo(this)
        }
        list.post { list.scrollToPosition(values.lastIndex) }
    }
    override fun getItemCount() = messages.size
    override fun getItemViewType(position: Int) = if (messages[position] is AiUserMessageDisplay) 0 else 1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val row = if (viewType == 0) AiUserMessageRow(context, action) else AiAssistantMessageRow(context, action)
        return Holder(row.apply { layoutParams = RecyclerView.LayoutParams(-1, -2) })
    }
    override fun onBindViewHolder(holder: Holder, position: Int) { (holder.itemView as AiMessageRow).bind(messages[position]) }
    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position)
        else payloads.forEach { if (it is CharSequence) (holder.itemView as AiMessageRow).updateText(messages[position]) }
    }
    companion object {
        const val COPY = 0x01010001
        const val PREVIEW = 0x01010002
        const val DELETE = 0x01010003
        const val MENU = 0x01010004
    }
}
