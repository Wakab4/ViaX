package moe.pure.browser.feature.passwords.ui

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.feature.library.data.NativeFaviconPaths
import moe.pure.browser.feature.passwords.model.PasswordEntry
import moe.pure.browser.feature.passwords.model.PasswordSites
import moe.pure.browser.feature.settings.skin.NativeSkinIcons
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.settings.NativeSettingViews
import moe.pure.browser.ui.theme.palette
import kotlin.math.abs

/** va.r0/xa.c use grouped metadata and selection by adapter position, including header offsets. */
internal class PasswordRecordAdapter(private val context: Context, private val clicked: (Int) -> Unit,
    private val held: ((View, Int) -> Boolean)?, private val selectableRows: Boolean = true) : RecyclerView.Adapter<PasswordRecordAdapter.Holder>() {
    sealed interface Row { data class Group(val site: String) : Row; data class Entry(val value: PasswordEntry) : Row }
    class Holder(view: View) : RecyclerView.ViewHolder(view)
    private val icons = NativeSkinIcons(context)
    private var fallback: Drawable? = null
    var rows: List<Row> = emptyList(); private set
    var selecting = false
    val selected = sortedSetOf<Int>()
    fun entry(position: Int) = (rows.getOrNull(position) as? Row.Entry)?.value
    fun entryPositions() = rows.indices.filter { rows[it] is Row.Entry }
    fun select(position: Int, value: Boolean) {
        val changed = if (value) selected.add(position) else selected.remove(position)
        if (changed) notifyItemChanged(position)
    }
    fun submit(entries: List<PasswordEntry>) {
        val next = ArrayList<Row>(); var previousSite: String? = null
        entries.sortedWith { a, b -> PasswordSites.siteOrder.compare(a.name, b.name) }.forEach {
            if (it.name != previousSite) { previousSite = it.name; next.add(Row.Group(it.name)) }; next.add(Row.Entry(it))
        }
        val before = rows
        if (before.isEmpty() != next.isEmpty()) { rows = next; notifyDataSetChanged(); return }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = before.size
            override fun getNewListSize() = next.size
            override fun areItemsTheSame(old: Int, new: Int): Boolean {
                val first = before[old]; val second = next[new]
                return if (first is Row.Entry && second is Row.Entry) first.value.id == second.value.id else first is Row.Group && second is Row.Group && first.site == second.site
            }
            override fun areContentsTheSame(old: Int, new: Int) = before[old] == next[new]
        }); rows = next; diff.dispatchUpdatesTo(this)
    }
    override fun getItemCount() = rows.size
    override fun getItemViewType(position: Int) = if (rows[position] is Row.Group) 0 else 1
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder((if (viewType == 0) NativeSettingViews.section(context)
        else if (selectableRows) RecordRow(context) else label(context).apply { setBackgroundResource(R.drawable.ref_redirect_page_feedback) }).apply { layoutParams = RecyclerView.LayoutParams(-1, -2) })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]
        if (row is Row.Group) { (holder.itemView as TextView).text = row.site; return }
        row as Row.Entry
        val root = holder.itemView
        val title = if (root is RecordRow) root.title else root as TextView
        title.apply {
            title.text = row.value.username
            val file = NativeFaviconPaths.direct(context, row.value.name)
            val drawable = if (file.exists) file.drawable else {
                if (fallback == null) fallback = (icons.resolve(R.drawable.ref_ic_webpage, "ic_webpage") ?: context.getDrawable(R.drawable.ref_ic_webpage))?.mutate()?.apply { setTint(context.palette.icon) }
                fallback
            }
            drawable?.setBounds(0, 0, dp(20), dp(20)); title.setCompoundDrawablesRelative(drawable, null, null, null)
        }
        if (root is RecordRow) { root.check.visibility = if (selecting) View.VISIBLE else View.GONE; root.check.isChecked = position in selected }
        var last: Long? = null
        root.setOnClickListener { val at = holder.bindingAdapterPosition; val now = SystemClock.elapsedRealtime(); if (at >= 0 && (last == null || abs(now - last!!) > 300)) { last = now; clicked(at) } }
        val longClick = held
        if (longClick == null) root.setOnLongClickListener(null) else root.setOnLongClickListener { val at = holder.bindingAdapterPosition; at >= 0 && longClick(it, at) }
    }
    private class RecordRow(context: Context) : LinearLayout(context) {
        val title = label(context)
        val check = CheckBox(context).apply { isClickable = false; isFocusable = false; buttonDrawable = context.getDrawable(R.drawable.dialog_checkbox); buttonTintList = null }
        init {
            setPaddingRelative(0, 0, dp(16), 0); setBackgroundResource(R.drawable.ref_redirect_page_feedback)
            addView(title, LayoutParams(0, -2, 1f)); addView(check, LayoutParams(-2, dp(16)).apply { setMargins(dp(2), dp(2), dp(2), dp(2)) })
        }
    }
    companion object {
        private fun label(context: Context) = NativeTypography.text(context).apply {
            setTextColor(context.palette.text); gravity = Gravity.CENTER_VERTICAL
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
            setSingleLine(); ellipsize = null; isHorizontalFadingEdgeEnabled = true; setFadingEdgeLength(dp(24)); textDirection = View.TEXT_DIRECTION_LOCALE
            compoundDrawablePadding = dp(16); setPaddingRelative(dp(16), dp(12), dp(16), dp(12))
        }
    }
}
