package moe.via.browser.browser.ui.menu.customize

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.content.Context
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import moe.via.browser.R
import moe.via.browser.feature.library.ui.recycler.LibraryRecyclerView
import moe.via.browser.ui.components.recycler.bindNativeItemClick
import moe.via.browser.ui.components.SkinIconBinder
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

/** i8.s uses a vertical five-span RecyclerView, including draggable settings and full-span headings. */
internal class MenuCustomizationList(context: Context, private val model: MenuCustomizationModel) : AutoCloseable {
    private val icons = SkinIconBinder(context)
    private var dragging = false
    private val adapter = Adapter()
    val view = LibraryRecyclerView(context).apply {
        id = R.id.settings_page_list
        layoutManager = GridLayoutManager(context, 5).apply { spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) = if (model.item(position) == null) 5 else 1
        } }
        this.adapter = this@MenuCustomizationList.adapter
        clipToPadding = false
    }
    private val touch = ItemTouchHelper(object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, holder: RecyclerView.ViewHolder): Int =
            makeMovementFlags(if (model.item(holder.bindingAdapterPosition) != null)
                ItemTouchHelper.UP or ItemTouchHelper.DOWN or ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT else 0, 0)
        override fun onMove(recyclerView: RecyclerView, holder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val from = holder.bindingAdapterPosition; val to = target.bindingAdapterPosition
            if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION || from == to) return false
            val item = model.item(from) ?: return false
            val moved = model.move(item, to)
            if (moved < 0) return false
            adapter.notifyItemMoved(from, moved); return true
        }
        override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = Unit
        override fun onSelectedChanged(holder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(holder, actionState); dragging = actionState != ItemTouchHelper.ACTION_STATE_IDLE
        }
    })
    init {
        touch.attachToRecyclerView(view)
    }
    fun refresh() { adapter.notifyDataSetChanged() }
    override fun close() { touch.attachToRecyclerView(null); view.adapter = null; view.dispose(); icons.close() }

    private inner class Adapter : RecyclerView.Adapter<Holder>() {
        override fun getItemCount() = model.rows.size
        override fun getItemViewType(position: Int) = if (model.item(position) == null) 0 else 1
        override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(NativeTypography.text(parent.context).apply {
            gravity = Gravity.CENTER
            if (type == 0) {
                setTextColor(context.palette.secondary)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
                setPadding(dp(16), 0, dp(16), 0)
                layoutParams = RecyclerView.LayoutParams(-1, -2).apply { topMargin = dp(16); bottomMargin = dp(16) }
            } else {
                setTextColor(context.palette.text)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.menu_label_size).toFloat())
                setLines(2); maxLines = 2; ellipsize = TextUtils.TruncateAt.END; compoundDrawablePadding = dp(2)
                setPadding(dp(5), dp(15), dp(5), dp(15)); background = context.touchFeedback(12)
                layoutParams = RecyclerView.LayoutParams(-1, -2)
            }
        })
        override fun onBindViewHolder(holder: Holder, position: Int) {
            when (val row = model.rows[position]) {
                is MenuCustomizationModel.Row.Heading -> holder.text.setText(if (row.displayed) R.string.ref_menu_custom_visible else R.string.ref_menu_custom_hidden)
                is MenuCustomizationModel.Row.Action -> {
                    val action = row.item.action
                    holder.text.text = action.title(holder.text.context)
                    icons.top(holder.text, action.icon, holder.text.dp(24), holder.text.context.palette.icon)
                    holder.text.bindNativeItemClick {
                        val from = holder.bindingAdapterPosition
                        if (from < 0 || dragging || view.itemAnimator?.isRunning == true) return@bindNativeItemClick
                        val moved = model.toggle(row.item)
                        if (moved >= 0) notifyItemMoved(from, moved)
                    }
                }
            }
        }
    }
    private class Holder(val text: TextView) : RecyclerView.ViewHolder(text)
}
