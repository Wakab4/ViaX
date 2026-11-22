package moe.via.browser.feature.scripts.popup.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.browser.navigation.AddressResolver
import moe.via.browser.ui.components.recycler.bindNativeItemClick
import moe.via.browser.feature.scripts.model.UserScript
import moe.via.browser.feature.scripts.popup.model.ScriptPopupModel
import moe.via.browser.feature.scripts.popup.model.ScriptPopupState
import moe.via.browser.feature.scripts.runtime.UserScriptBridge
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.webview.compat.NativePageAddress

internal class ScriptPopupListAdapter(private val model: ScriptPopupModel) : RecyclerView.Adapter<ScriptPopupListAdapter.Holder>() {
    class Holder(val row: ScriptPopupViews.CheckRow) : RecyclerView.ViewHolder(row)
    private var scripts = emptyList<UserScript>()
    private var bound = false
    fun submit(state: ScriptPopupState) {
        if (scripts === state.scripts && bound == state.bound) return
        val previous = scripts
        val same = bound == state.bound && previous.map { it.id } == state.scripts.map { it.id }
        scripts = state.scripts; bound = state.bound
        val changed = state.changedId?.let { id -> scripts.indexOfFirst { it.id == id } } ?: -1
        if (same && changed >= 0) notifyItemChanged(changed) else notifyDataSetChanged()
    }
    override fun getItemCount() = scripts.size
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(ScriptPopupViews.CheckRow(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(-1, -2)
    })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val script = scripts[position]
        holder.row.title.text = script.name
        holder.row.isEnabled = bound
        holder.row.check.apply {
            setOnCheckedChangeListener(null); isChecked = script.enabled; isEnabled = bound; contentDescription = script.name
            setOnCheckedChangeListener { _, checked -> model.toggle(script.id, checked) }
        }
        holder.row.bindNativeItemClick { if (bound) model.select(script.id) }
    }
}

internal class ScriptPopupActionsAdapter(private val model: ScriptPopupModel) : RecyclerView.Adapter<ScriptPopupActionsAdapter.Holder>() {
    class Holder(val text: TextView) : RecyclerView.ViewHolder(text)
    private data class Row(val kind: Int, val text: String, val command: UserScriptBridge.MenuCommand? = null)
    private var rows = emptyList<Row>()
    private var bound = false
    private var selectedId: Long? = null
    private var commands = emptyList<UserScriptBridge.MenuCommand>()
    fun submit(context: Context, state: ScriptPopupState) {
        val selected = state.selected
        val selectedCommands = state.commands.filter { it.scriptKey == selected?.key }
        // ua.i.b3 changes only the first label. The exclusion row is rebuilt by a3,
        // when selection/commands change, rather than as a side effect of toggling.
        if (state.changedId != null && rows.isNotEmpty() && bound == state.bound && selectedId == selected?.id &&
            commands == selectedCommands && state.changedId == selected?.id) {
            val label = context.getString(if (selected?.enabled == true) R.string.ref_script_popup_enabled else R.string.ref_script_popup_disabled)
            rows = rows.toMutableList().apply { this[0] = this[0].copy(text = label) }
            notifyItemChanged(0)
            return
        }
        val next = buildList {
            add(Row(2, context.getString(if (selected?.enabled == true) R.string.ref_script_popup_enabled else R.string.ref_script_popup_disabled)))
            add(Row(1, context.getString(R.string.ref_script_popup_edit)))
            val host = NativePageAddress.authority(state.url)
            if (selected?.enabled == true && AddressResolver.isWebUrl(state.url.orEmpty()) && host.length > 3)
                add(Row(4, context.getString(R.string.ref_script_popup_exclude, host)))
            add(Row(3, context.getString(R.string.ref_script_popup_manager)))
            selectedCommands.forEach { add(Row(-1, it.label, it)) }
        }
        selectedId = selected?.id; commands = selectedCommands
        if (rows == next && bound == state.bound) return
        val toggleOnly = bound == state.bound && rows.size == next.size && rows.drop(1) == next.drop(1) && rows.isNotEmpty()
        rows = next; bound = state.bound
        if (toggleOnly) notifyItemChanged(0) else notifyDataSetChanged()
    }
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(ScriptPopupViews.text(parent.context).apply {
        gravity = android.view.Gravity.CENTER_VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16)); background = context.touchFeedback()
        layoutParams = RecyclerView.LayoutParams(-1, -2)
    })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        holder.text.text = row.text; holder.text.isEnabled = bound
        holder.text.bindNativeItemClick {
            if (bound) when (row.kind) {
                1 -> model.edit()
                2 -> model.state.value?.selected?.let { model.toggle(it.id, !it.enabled) }
                3 -> model.manager()
                4 -> model.exclude()
                -1 -> row.command?.let(model::command)
            }
        }
    }
}
