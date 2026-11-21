package moe.pure.browser.feature.library.forms

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.library.ui.recycler.bindNativeItemClick
import moe.pure.browser.feature.settings.skin.NativeSkinIcons
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.touchFeedback
import moe.pure.browser.ui.theme.palette

/** a8.f/n: one highlighted directory, depth indentation, and the optional final creation row. */
internal class BookmarkFolderAdapter(private val context: Context, private val inline: Boolean,
    private val selected: (String) -> Unit, private val create: () -> Unit) : RecyclerView.Adapter<BookmarkFolderAdapter.Holder>() {
    class Holder(val text: TextView) : RecyclerView.ViewHolder(text)
    private val icons = NativeSkinIcons(context)
    private var rows = emptyList<BookmarkFolderChoice>()
    private var selectedId = ""
    private var creation = false

    fun submit(values: List<BookmarkFolderChoice>, current: String, canCreate: Boolean) {
        val old = rows
        val oldSelection = selectedId
        val oldCreation = creation
        rows = values; selectedId = current; creation = canCreate
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size + if (oldCreation) 1 else 0
            override fun getNewListSize() = rows.size + if (creation) 1 else 0
            override fun areItemsTheSame(a: Int, b: Int) = if (a == old.size || b == rows.size) a == old.size && b == rows.size
                else old[a].entry.id == rows[b].entry.id
            override fun areContentsTheSame(a: Int, b: Int) = if (a == old.size || b == rows.size) true
                else old[a] == rows[b] && (old[a].entry.id == oldSelection) == (rows[b].entry.id == selectedId)
        }).dispatchUpdatesTo(this)
    }
    override fun getItemCount() = rows.size + if (creation) 1 else 0
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(NativeTypography.text(parent.context).apply {
        layoutParams = RecyclerView.LayoutParams(-1, -2)
        textSize = 14f; setTextColor(context.palette.text)
        setSingleLine(true); ellipsize = null; setHorizontallyScrolling(true)
        isHorizontalFadingEdgeEnabled = true; setFadingEdgeLength(dp(24))
        textDirection = android.view.View.TEXT_DIRECTION_LOCALE; compoundDrawablePadding = dp(16)
    })
    override fun onBindViewHolder(holder: Holder, position: Int) = bind(holder, position)
    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) = bind(holder, position)
    private fun bind(holder: Holder, position: Int) {
        val choice = rows.getOrNull(position)
        val text = holder.text
        text.compoundDrawablePadding = text.dp(if (choice == null) 18 else 16)
        text.text = if (choice == null) context.getString(R.string.ref_bookmark_form_new_folder)
            else if (choice.entry.id.isEmpty()) context.getString(R.string.ref_bookmark_form_root) else choice.entry.title
        text.setPaddingRelative(text.dp(16) * if (choice == null) 1 else minOf(10, choice.depth + 1), text.dp(14), text.dp(16), text.dp(14))
        val checked = choice?.entry?.id == selectedId
        text.background = if (checked) {
            if (inline) GradientDrawable().apply { setColor(0x30808080); cornerRadius = text.dp(18).toFloat() }
            else ColorDrawable(0x30808080)
        } else context.touchFeedback()
        val resource = if (choice == null) R.drawable.ref_ic_folder_add else R.drawable.ref_ic_folder
        val icon = (icons.resolve(resource) ?: context.getDrawable(resource))?.mutate()?.apply {
            setTint(context.palette.text); setBounds(0, 0, text.dp(22), text.dp(22))
        }
        text.setCompoundDrawablesRelative(icon, null, null, null)
        text.bindNativeItemClick { if (choice == null) create() else if (choice.entry.id != selectedId) selected(choice.entry.id) }
    }
}
