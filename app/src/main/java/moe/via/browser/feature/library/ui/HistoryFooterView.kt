package moe.via.browser.feature.library.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

/** db.n's five a0 actions keep their left/right groups and selected/disabled state independently. */
internal class HistoryFooterView(context: Context, select: () -> Unit, delete: () -> Unit, edit: () -> Unit) : LinearLayout(context) {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x30808080 }
    private val tabs = button(R.string.ref_history_footer_tabs, select)
    private val all = button(R.string.ref_history_footer_select_all, select)
    private val remove = button(R.string.ref_library_context_delete, delete, destructive = true)
    private val clear = button(R.string.ref_closed_tabs_clear, delete, destructive = true)
    private val done = button(R.string.ref_history_footer_edit, edit)
    val clearAnchor: View get() = clear
    private var clearEnabled = false
    private var editEnabled = true
    init {
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setWillNotDraw(false)
        addView(tabs); addView(all); addView(remove)
        addView(View(context), LayoutParams(0, context.dp(48), 1f))
        addView(clear); addView(done)
        render(false, 0, 0)
    }
    fun rows(empty: Boolean, queryEmpty: Boolean) {
        if (queryEmpty) { clearEnabled = !empty; editEnabled = !empty }
    }
    fun render(editing: Boolean, selected: Int, count: Int) {
        tabs.visibility = if (editing) GONE else VISIBLE
        all.visibility = if (editing) VISIBLE else GONE
        remove.visibility = if (editing) VISIBLE else GONE
        clear.visibility = if (editing) GONE else VISIBLE
        all.setText(if (selected > 0 && selected == count) R.string.ref_history_footer_cancel_all else R.string.ref_history_footer_select_all)
        remove.text = if (selected > 0) context.getString(R.string.ref_history_footer_delete_count, selected) else context.getString(R.string.ref_library_context_delete)
        remove.isEnabled = selected > 0; remove.isSelected = selected > 0
        clear.isEnabled = clearEnabled
        done.setText(if (editing) R.string.ref_history_footer_done else R.string.ref_history_footer_edit)
        done.isEnabled = editing || editEnabled
    }
    private fun button(label: Int, action: () -> Unit, destructive: Boolean = false) = NativeTypography.text(context).apply {
        setText(label); textSize = 14f; gravity = Gravity.CENTER
        val normal = context.palette.text
        setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_enabled, android.R.attr.state_selected), intArrayOf()),
            intArrayOf((normal and 0xFFFFFF) or 0x80000000.toInt(), if (destructive) 0xFFD64146.toInt() else context.palette.accent, normal)))
        setPadding(context.dp(16), 0, context.dp(16), 0); minimumHeight = context.dp(48)
        background = context.touchFeedback(18); setOnClickListener { action() }; layoutParams = LayoutParams(-2, -2)
    }
    override fun onDraw(canvas: Canvas) { super.onDraw(canvas); canvas.drawRect(0f, 0f, width.toFloat(), 2f, line) }
}
