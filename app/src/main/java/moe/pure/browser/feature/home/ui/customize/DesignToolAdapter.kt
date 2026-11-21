package moe.pure.browser.feature.home.ui.customize

import android.content.Context
import android.graphics.Typeface
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.theme.palette

/** f9.b owns one selected position and rebinds only the old/new positions. */
internal class DesignToolAdapter(private val context: Context,
    private val clicked: (DesignTool, Int) -> Unit) : RecyclerView.Adapter<DesignToolAdapter.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.home_design_tool_icon)
        val title: TextView = view.findViewById(R.id.home_design_tool_title)
        val detail: TextView = view.findViewById(R.id.home_design_tool_detail)
    }
    private var rows: List<DesignTool> = emptyList()
    var selectedPosition = -1
        private set
    fun submit(value: List<DesignTool>) { rows = value; notifyDataSetChanged() }
    fun selected(position: Int) {
        val before = selectedPosition
        if (before == position) return
        selectedPosition = position
        if (before >= 0 && before < itemCount) notifyItemChanged(before)
        if (position >= 0 && position < itemCount) notifyItemChanged(position)
    }
    fun changed(position: Int) { notifyItemChanged(position) }
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(context).inflate(R.layout.home_design_tool_item, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val tool = rows[position]
        val selected = position == selectedPosition
        holder.image.apply {
            visibility = if (tool.icon == 0) View.GONE else View.VISIBLE
            setImageResource(tool.icon); setColorFilter(if (selected) context.palette.accent else context.palette.icon)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        holder.title.text = tool.title
        val detail = tool.summary
        holder.detail.text = detail; holder.detail.visibility = if (detail == null) View.GONE else View.VISIBLE
        for (text in arrayOf(holder.title, holder.detail)) {
            text.isSelected = selected
            text.setTextColor(if (selected) context.palette.accent else context.palette.text)
            NativeTypography.style(text, if (selected) Typeface.BOLD else Typeface.NORMAL)
        }
        var last: Long? = null
        holder.itemView.setOnClickListener {
            val current = holder.bindingAdapterPosition
            val now = SystemClock.elapsedRealtime()
            if (current != RecyclerView.NO_POSITION && (last == null || kotlin.math.abs(now - last!!) > 300)) {
                last = now; clicked(tool, current)
            }
        }
    }
}
