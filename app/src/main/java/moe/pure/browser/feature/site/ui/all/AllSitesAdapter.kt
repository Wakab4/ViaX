package moe.pure.browser.feature.site.ui.all

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.feature.library.ui.recycler.bindNativeItemClick
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** hb.w6/b6.c.h is a plain single-line row, without an enabled/disabled subtitle. */
internal class AllSitesAdapter(private val context: Context, private val clicked: (String) -> Unit,
    private val contextMenu: (View, String) -> Unit) : RecyclerView.Adapter<AllSitesAdapter.Holder>() {
    class Holder(val text: TextView) : RecyclerView.ViewHolder(text)
    private var rows = emptyList<String>()
    fun submit(value: List<String>) {
        val previous = rows
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = value.size
            override fun areItemsTheSame(old: Int, new: Int) = previous[old] == value[new]
            override fun areContentsTheSame(old: Int, new: Int) = true
            override fun getChangePayload(old: Int, new: Int): Any = Any()
        })
        rows = value; diff.dispatchUpdatesTo(this)
    }
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(NativeTypography.text(context).apply {
        setTextColor(context.palette.text); textSize = 14f; gravity = Gravity.CENTER_VERTICAL
        setSingleLine(true); ellipsize = null; textDirection = View.TEXT_DIRECTION_LOCALE
        isHorizontalFadingEdgeEnabled = true; setFadingEdgeLength(dp(24))
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setBackgroundResource(moe.pure.browser.R.drawable.ref_tab_switcher_row_feedback)
        layoutParams = RecyclerView.LayoutParams(-1, -2)
    })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.text.text = rows[position]
        holder.text.bindNativeItemClick { rows.getOrNull(holder.bindingAdapterPosition)?.let(clicked) }
        holder.text.setOnLongClickListener {
            rows.getOrNull(holder.bindingAdapterPosition)?.let { contextMenu(holder.text, it) }; true
        }
    }
}
