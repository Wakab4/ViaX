package moe.via.browser.browser.ui.menu.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.ui.components.recycler.bindNativeItemClick
import moe.via.browser.browser.ui.menu.MenuEntry
import moe.via.browser.ui.components.SkinIconBinder
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

/** h8.c; the null entry is Native ID 22, which occupies an adapter slot but is GONE. */
internal class MenuGridAdapter(private val cells: List<MenuEntry?>, private val icons: SkinIconBinder,
    private val clicked: (MenuEntry, Boolean) -> Unit) : RecyclerView.Adapter<MenuGridAdapter.Holder>() {
    class Holder(val text: TextView) : RecyclerView.ViewHolder(text)
    override fun getItemCount() = cells.size
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(NativeTypography.text(parent.context).apply {
        val measured = parent.measuredWidth / 5
        layoutParams = RecyclerView.LayoutParams(if (measured > 0) measured else dp(64), -2)
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.menu_label_size).toFloat())
        gravity = Gravity.CENTER; setLines(2); maxLines = 2; ellipsize = TextUtils.TruncateAt.END
        compoundDrawablePadding = dp(2); setPadding(dp(5), dp(15), dp(5), dp(15)); background = context.touchFeedback(12)
    })
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val entry = cells[position]
        val view = holder.text
        if (entry == null) { view.visibility = View.GONE; return }
        view.visibility = View.VISIBLE
        view.text = entry.title; view.contentDescription = entry.description
        view.setTextColor(if (entry.selected) view.context.palette.accent else view.context.palette.text)
        NativeTypography.style(view, if (entry.selected) Typeface.BOLD else Typeface.NORMAL)
        icons.top(view, entry.action.icon, view.dp(24), if (entry.selected) view.context.palette.accent else view.context.palette.icon)
        view.isEnabled = entry.enabled; view.alpha = if (entry.enabled) 1f else .35f
        view.bindNativeItemClick { clicked(entry, false) }
        view.setOnLongClickListener { clicked(entry, true); true }
    }
}
