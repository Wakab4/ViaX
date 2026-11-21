package moe.pure.browser.feature.settings.skin.ui

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.feature.settings.skin.NativeSkinEntry
import moe.pure.browser.ui.settings.NativeSettingViews

internal class SkinSettingsAdapter(private val context: Context, private val rows: List<NativeSkinEntry>,
    private val selection: () -> String, private val clicked: (NativeSkinEntry, Int) -> Unit,
    private val held: (NativeSkinEntry, Int) -> Boolean) : RecyclerView.Adapter<SkinSettingsAdapter.Holder>() {
    class Holder(val row: NativeSettingViews.Radio) : RecyclerView.ViewHolder(row)
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(NativeSettingViews.Radio(context).apply {
        layoutParams = RecyclerView.LayoutParams(-1, -2)
    })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = rows[position]
        holder.row.check.isChecked = entry.name == selection()
        holder.row.title.text = entry.label
        holder.row.detail.visibility = View.GONE
        var last: Long? = null
        holder.row.setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            if (last == null || kotlin.math.abs(now - last!!) > 300L) {
                last = now
                val current = holder.bindingAdapterPosition
                if (current != RecyclerView.NO_POSITION) clicked(entry, current)
            }
        }
        holder.row.setOnLongClickListener {
            val current = holder.bindingAdapterPosition
            current != RecyclerView.NO_POSITION && held(entry, current)
        }
    }
}
