package moe.via.browser.feature.ai.ui.chat

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Rect
import android.text.TextUtils
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.feature.ai.model.AiPageAttachment
import moe.via.browser.feature.ai.model.AiPrompt
import moe.via.browser.feature.ai.model.AiPromptChoice
import moe.via.browser.ui.components.recycler.bindNativeItemClick
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.via.browser.ui.theme.palette

/** a2.b and c9.e/s/p: a horizontal list owns touch input and diffs two actual row types. */
internal class AiPromptStrip(context: Context, private val select: (String) -> Unit, private val remove: () -> Unit) : RecyclerView(context), AutoCloseable {
    private sealed interface Item {
        data class Attachment(val value: AiPageAttachment) : Item
        data class Prompt(val value: AiPrompt, val selected: Boolean) : Item
    }
    private class Holder(view: View) : ViewHolder(view)
    private var items: List<Item> = emptyList()
    private var messageGeneration = 0
    private val attachments = ArrayList<AiAttachmentChip>()
    private val rows = object : Adapter<Holder>() {
        override fun getItemCount() = items.size
        override fun getItemViewType(position: Int) = if (items[position] is Item.Attachment) 0 else 1
        override fun onCreateViewHolder(parent: ViewGroup, type: Int): Holder {
            val row = if (type == 0) AiAttachmentChip(context, remove).also(attachments::add) else context.chatText(size = 12f).apply {
                gravity = Gravity.CENTER; setSingleLine(); ellipsize = TextUtils.TruncateAt.END; maxWidth = dp(96)
                setPadding(dp(9), 0, dp(9), 0); background = context.chatPromptBackground()
                setTextColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_selected), intArrayOf(android.R.attr.state_enabled, android.R.attr.state_checked), intArrayOf()),
                    intArrayOf(context.palette.accent, context.palette.accent, context.palette.text)))
            }
            return Holder(row.apply { layoutParams = LayoutParams(-2, -1) })
        }
        override fun onBindViewHolder(holder: Holder, position: Int) {
            when (val item = items[position]) {
                is Item.Attachment -> (holder.itemView as AiAttachmentChip).bind(item.value)
                is Item.Prompt -> (holder.itemView as TextView).apply {
                    text = if (item.value.id == AiPrompt.DEFAULT_ID) context.getString(R.string.ref_ai_dialog_chat_any) else item.value.name
                    isSelected = item.selected
                    bindNativeItemClick { if (!item.selected) select(item.value.id) }
                }
            }
        }
    }
    init {
        layoutManager = LinearLayoutManager(context, HORIZONTAL, false)
        clipToPadding = false; setPadding(dp(12), 0, dp(12), 0)
        NativeRecyclerEdgeEffect.install(this)
        addItemDecoration(object : ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) { outRect.set(dp(4), 0, dp(4), 0) }
        })
        adapter = rows
    }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean { parent.requestDisallowInterceptTouchEvent(true); return super.dispatchTouchEvent(event) }
    fun submit(attachment: AiPageAttachment?, choices: List<AiPromptChoice>, generation: Int) {
        val before = items
        val next = buildList<Item> { if (attachment != null) add(Item.Attachment(attachment)); choices.forEach { add(Item.Prompt(it.prompt, it.selected)) } }
        var reset = next.isEmpty() != before.isEmpty()
        if (!reset) reset = generation == messageGeneration
        messageGeneration = generation
        if (reset) { items = next; rows.notifyDataSetChanged(); visibility = if (next.isEmpty()) GONE else VISIBLE; return }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = before.size
            override fun getNewListSize() = next.size
            override fun areItemsTheSame(old: Int, new: Int): Boolean = when (val item = before[old]) {
                is Item.Attachment -> next[new] is Item.Attachment
                is Item.Prompt -> (next[new] as? Item.Prompt)?.value?.id == item.value.id
            }
            override fun areContentsTheSame(old: Int, new: Int): Boolean = when (val item = before[old]) {
                is Item.Attachment -> (next[new] as? Item.Attachment)?.value?.title == item.value.title
                is Item.Prompt -> (next[new] as? Item.Prompt)?.let { it.value.name == item.value.name && it.selected == item.selected } == true
            }
            override fun getChangePayload(old: Int, new: Int): Any? = if (before[old]::class == next[new]::class) Any() else null
        })
        items = next; diff.dispatchUpdatesTo(rows); visibility = if (next.isEmpty()) GONE else VISIBLE
    }
    override fun close() { stopScroll(); itemAnimator?.endAnimations(); (edgeEffectFactory as? NativeRecyclerEdgeEffect)?.close(); attachments.forEach(AiAttachmentChip::close); attachments.clear(); adapter = null }
}
