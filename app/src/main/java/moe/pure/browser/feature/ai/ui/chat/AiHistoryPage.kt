package moe.pure.browser.feature.ai.ui.chat

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import moe.pure.browser.R
import moe.pure.browser.feature.ai.model.AiThread
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.recycler.NativeRangeSelection
import moe.pure.browser.ui.components.recycler.NativeRecyclerView

internal class AiHistoryPage(context: Context, private val openThread: (String) -> Unit, private val newThread: () -> Unit,
    private val settings: () -> Unit, private val deleteThreads: (List<AiThread>) -> Unit,
    private val selectionChanged: (Boolean) -> Unit) : LinearLayout(context), AutoCloseable {
    private val selected = linkedSetOf<Int>()
    private val list = NativeRecyclerView(context).apply { id = R.id.ai_chat_threads_list; layoutManager = LinearLayoutManager(context); clipToPadding = false }
    private val adapter: AiThreadAdapter = AiThreadAdapter(context, ::clicked) { position ->
        if (!selecting) { selecting = true; selectionChanged(true) }
        range.start(position); true
    }
    private val title = context.chatText(context.getString(R.string.ref_ai_dialog_all_topics), 16f, true).apply { chatTitle() }
    private val close = context.chatIcon(R.drawable.ref_ic_close, context.getString(android.R.string.cancel)) { cancelSelection() }
    private val configure = context.chatIcon(R.drawable.ref_ai_chat_settings, context.getString(R.string.ref_ai_page_settings), settings)
    private val action = context.chatIcon(R.drawable.ref_ic_add, context.getString(R.string.ref_ai_dialog_new_topic), ::performAction)
    private val empty = context.chatText("¯\\_(ツ)_/¯", 22f).apply { contentDescription = context.getString(R.string.ref_ai_page_empty); gravity = Gravity.CENTER; visibility = GONE }
    private val range: NativeRangeSelection = NativeRangeSelection(list, object : NativeRangeSelection.Actions {
        override fun selectable(position: Int) = selecting && position in adapter.threads.indices
        override fun selected(position: Int) = position in selected
        override fun select(position: Int, value: Boolean) {
            if (value) selected.add(position) else selected.remove(position)
            adapter.submit(adapter.threads, selected); updateToolbar()
        }
    })
    var selecting = false
        private set
    init {
        orientation = VERTICAL
        addView(LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(close, LayoutParams(dp(54), dp(48)).apply { marginStart = dp(3) })
            addView(title, LayoutParams(0, -2, 1f).apply { marginStart = dp(16) })
            addView(configure, LayoutParams(dp(54), dp(48)).apply { marginEnd = dp(3) })
            addView(action, LayoutParams(dp(54), dp(48)).apply { marginEnd = dp(3) })
        }, LayoutParams(-1, -2).apply { topMargin = dp(7) })
        addView(empty, LayoutParams(-1, 0, 1f))
        list.adapter = adapter; addView(list, LayoutParams(-1, 0, 1f)); updateToolbar()
    }
    fun submit(threads: List<AiThread>) {
        selected.retainAll(threads.indices.toSet()); adapter.submit(threads, selected)
        empty.visibility = if (threads.isEmpty()) VISIBLE else GONE; list.visibility = if (threads.isEmpty()) GONE else VISIBLE
        if (selecting && selected.isEmpty()) cancelSelection() else updateToolbar()
    }
    private fun clicked(position: Int) {
        if (!selecting) { openThread(adapter.threads[position].id); return }
        if (!selected.add(position)) selected.remove(position)
        if (selected.isEmpty()) cancelSelection() else { adapter.submit(adapter.threads, selected); updateToolbar() }
    }
    fun cancelSelection(): Boolean {
        if (!selecting) return false
        selecting = false; selected.clear(); range.finish(); adapter.submit(adapter.threads, selected); updateToolbar(); selectionChanged(false); return true
    }
    private fun performAction() { if (!selecting) newThread() else deleteThreads(adapter.threads.filterIndexed { index, _ -> index in selected }) }
    private fun updateToolbar() {
        close.visibility = if (selecting) VISIBLE else GONE; configure.visibility = if (selecting) GONE else VISIBLE
        title.text = if (selecting) selected.size.toString() else context.getString(R.string.ref_ai_dialog_all_topics)
        action.chatImage(if (selecting) R.drawable.ref_ic_menu_delete else R.drawable.ref_ic_add)
        action.contentDescription = context.getString(if (selecting) R.string.ref_ai_dialog_delete else R.string.ref_ai_dialog_new_topic)
    }
    override fun close() { range.close(); list.dispose(); list.adapter = null }
}
