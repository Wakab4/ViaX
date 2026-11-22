package moe.via.browser.feature.library.popup.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.feature.library.popup.BookmarkPopupFolderRow
import moe.via.browser.ui.theme.skin.NativeSkinIcons
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette
import java.util.WeakHashMap

/** a8.f/b: selected flat rows, original depth indentation, and content-diff payloads. */
internal class BookmarkPopupFolderAdapter(private val context: Context, private val choose: (String) -> Unit) :
    RecyclerView.Adapter<BookmarkPopupFolderAdapter.Holder>(), AutoCloseable {
    class Holder(val label: TextView) : RecyclerView.ViewHolder(label)
    private var rows = emptyList<BookmarkPopupFolderRow>()
    private var padding = 14
    private var closed = false
    private val labels = WeakHashMap<TextView, Unit>()
    private val skin = NativeSkinIcons(context)

    fun submit(next: List<BookmarkPopupFolderRow>) {
        if (closed) return
        val previous = rows
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = previous.size
            override fun getNewListSize() = next.size
            override fun areItemsTheSame(old: Int, new: Int) = previous[old].entry.id == next[new].entry.id
            override fun areContentsTheSame(old: Int, new: Int) = previous[old] == next[new]
            override fun getChangePayload(old: Int, new: Int): Any = Any()
        })
        padding = 14 - if (next.size > 6) minOf((next.size - 6) / 2, 8) else 0
        rows = next
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(NativeTypography.text(context).apply {
        layoutParams = RecyclerView.LayoutParams(-1, -2)
        setTextColor(context.palette.text)
        textSize = 14f
        setSingleLine(true)
        ellipsize = null
        setFadingEdgeLength(dp(24))
        isHorizontalFadingEdgeEnabled = true
        textDirection = View.TEXT_DIRECTION_LOCALE
        compoundDrawablePadding = dp(16)
        if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
        labels[this] = Unit
        icon(this)
    })

    override fun getItemCount() = rows.size
    override fun onBindViewHolder(holder: Holder, position: Int) = bind(holder, rows[position])
    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) = bind(holder, rows[position])

    private fun bind(holder: Holder, row: BookmarkPopupFolderRow) = with(holder.label) {
        text = if (row.entry.id.isEmpty()) context.getString(R.string.ref_bookmark_popup_root) else row.entry.title
        setPaddingRelative(dp(16) * minOf(10, row.depth + 1), dp(padding), dp(16), dp(padding))
        background = if (row.selected) ColorDrawable(0x30808080) else StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed), ColorDrawable(0x30808080))
            addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
        }
        var clickedAt: Long? = null
        setOnClickListener {
            val now = SystemClock.elapsedRealtime()
            val previous = clickedAt
            if (!closed && (previous == null || kotlin.math.abs(now - previous) > 300L)) { clickedAt = now; choose(row.entry.id) }
        }
    }

    private fun icon(label: TextView) {
        val drawable = (skin.resolve(R.drawable.ref_ic_folder) ?: context.getDrawable(R.drawable.ref_ic_folder))?.mutate()
        drawable?.setTint(context.palette.text)
        drawable?.setBounds(0, 0, label.dp(22), label.dp(22))
        label.setCompoundDrawablesRelative(drawable, null, null, null)
    }

    override fun close() {
        if (closed) return
        closed = true
        labels.keys.toList().forEach { it.setOnClickListener(null); it.setCompoundDrawables(null, null, null, null) }
        labels.clear()
    }
}
