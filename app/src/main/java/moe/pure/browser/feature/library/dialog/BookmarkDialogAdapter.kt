package moe.pure.browser.feature.library.dialog

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.settings.skin.NativeSkinIcons
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.touchFeedback
import moe.pure.browser.ui.theme.palette
import moe.pure.browser.feature.library.ui.recycler.bindNativeItemClick
import moe.pure.browser.feature.library.data.NativeFaviconPaths

/** a8.k.q3 deliberately replaces the whole compact list, unlike the full manager's DiffUtil. */
internal class BookmarkDialogAdapter(private val activity: Activity) : RecyclerView.Adapter<BookmarkDialogAdapter.Holder>(), AutoCloseable {
    class Holder(val text: TextView) : RecyclerView.ViewHolder(text)
    var clicked: (BookmarkDialogRow) -> Unit = {}
    var rows: List<BookmarkDialogRow> = emptyList()
        set(value) { field = value; notifyDataSetChanged() }
    private var closed = false
    private object IconsChanged
    private fun iconsChanged() { if (!closed && itemCount > 0) notifyItemRangeChanged(0, itemCount, IconsChanged) }
    private val icons = NativeSkinIcons(activity)
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(NativeTypography.text(activity).apply {
        layoutParams = RecyclerView.LayoutParams(-1, -2)
        gravity = Gravity.CENTER_VERTICAL; textSize = 14f; setTextColor(activity.palette.text)
        setPadding(dp(16), dp(12), dp(16), dp(12)); textDirection = android.view.View.TEXT_DIRECTION_LOCALE
        setSingleLine(); ellipsize = null; isHorizontalFadingEdgeEnabled = true; setFadingEdgeLength(dp(24))
        compoundDrawablePadding = dp(16); background = activity.touchFeedback()
    })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val row = rows[position]; val text = holder.text
        text.bindNativeItemClick { if (!closed) rows.getOrNull(holder.bindingAdapterPosition)?.let(clicked) }
        val title = row.entry.title
        text.text = if (title.length > 256) title.substring(0, 256) + "..." else title
        bindIcon(text, row)
    }
    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position)
        else rows.getOrNull(position)?.let { bindIcon(holder.text, it) }
    }
    private fun bindIcon(text: TextView, row: BookmarkDialogRow) {
        val file = if (row.entry.isFolder) null else NativeFaviconPaths.direct(activity, row.entry.url)
        val drawable = if (file?.exists == true) file.drawable else {
            val resource = if (row.entry.isFolder) R.drawable.ref_ic_folder else R.drawable.ref_ic_bookmark
            (icons.resolve(resource) ?: activity.getDrawable(resource))?.mutate()?.apply { setTint(activity.palette.icon) }
        }
        drawable?.setBounds(0, 0, activity.dp(20), activity.dp(20))
        text.setCompoundDrawablesRelative(drawable, null, null, null)
    }
    fun refresh() { iconsChanged() }
    override fun close() { closed = true; clicked = {}; rows = emptyList() }
}
