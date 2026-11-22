package moe.via.browser.feature.search.ui.settings

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.feature.search.model.SearchEngine
import moe.via.browser.ui.settings.NativeSettingViews
import kotlin.math.abs

internal class SearchEngineListAdapter(private val context: Context,
    private val selected: (SearchEngine) -> Unit, private val actions: (View, Int) -> Unit) : RecyclerView.Adapter<SearchEngineListAdapter.Holder>() {
    class Holder(val row: NativeSettingViews.Radio) : RecyclerView.ViewHolder(row)
    val entries = ArrayList<SearchEngine>()
    var selectedId = 0
    var shortcuts: Map<Int, String> = emptyMap()
    override fun getItemCount() = entries.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(NativeSettingViews.Radio(context).apply { layoutParams = RecyclerView.LayoutParams(-1, -2) })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val engine = entries[position]
        holder.row.title.text = engine.name
        holder.row.detail.text = shortcuts[engine.id].orEmpty()
        holder.row.detail.visibility = if (holder.row.detail.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.row.check.isChecked = engine.id == selectedId
        var last: Long? = null
        holder.row.setOnClickListener {
            val at = holder.bindingAdapterPosition
            val now = SystemClock.elapsedRealtime()
            if (at >= 0 && (last == null || abs(now - last!!) > 300)) { last = now; selected(entries[at]) }
        }
        holder.row.setOnLongClickListener { val at = holder.bindingAdapterPosition; if (at < 0) false else { actions(it, at); true } }
    }
    fun position(id: Int) = entries.indexOfFirst { it.id == id }
    fun select(id: Int) {
        val previous = position(selectedId); selectedId = id
        if (previous >= 0) notifyItemChanged(previous)
        val current = position(id); if (current >= 0) notifyItemChanged(current)
    }
}
