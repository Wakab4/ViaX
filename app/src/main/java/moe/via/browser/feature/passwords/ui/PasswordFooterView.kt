package moe.via.browser.feature.passwords.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Space
import moe.via.browser.R
import moe.via.browser.ui.theme.fonts.NativeTypography
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** va.r0's left select/delete group and independent right edit action. */
internal class PasswordFooterView(context: Context, all: () -> Unit, remove: () -> Unit, edit: () -> Unit) : LinearLayout(context) {
    private val line = Paint().apply { color = 0x30808080 }
    private val select = button(R.string.ref_password_page_select_all, all)
    private val delete = button(R.string.ref_search_page_delete, remove, true)
    private val editing = button(R.string.ref_search_page_edit, edit)
    init {
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setWillNotDraw(false)
        addView(select); addView(delete); addView(Space(context), LayoutParams(0, dp(48), 1f)); addView(editing)
        render(false, 0, 0)
    }
    fun render(mode: Boolean, selected: Int, count: Int) {
        select.visibility = if (mode) View.VISIBLE else View.GONE; delete.visibility = select.visibility
        select.setText(if (selected > 0 && selected == count) R.string.ref_password_page_cancel_all else R.string.ref_password_page_select_all)
        delete.text = if (selected == 0) context.getString(R.string.ref_search_page_delete) else context.getString(R.string.ref_password_page_delete_count, selected)
        delete.isEnabled = selected > 0; delete.isSelected = selected > 0
        editing.setText(if (mode) R.string.ref_password_page_done else R.string.ref_search_page_edit); editing.isEnabled = count > 0
    }
    private fun button(label: Int, action: () -> Unit, destructive: Boolean = false) = NativeTypography.text(context).apply {
        setText(label); gravity = Gravity.CENTER
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(R.dimen.dialog_body_size).toFloat())
        val normal = context.palette.text
        setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_enabled, android.R.attr.state_selected), intArrayOf()),
            intArrayOf((normal and 0xFFFFFF) or 0x80000000.toInt(), if (destructive) 0xFFD64146.toInt() else context.palette.accent, normal)))
        setPadding(dp(16), 0, dp(16), 0); minimumHeight = dp(48); setBackgroundResource(R.drawable.ref_redirect_page_feedback)
        setOnClickListener { action() }; layoutParams = LayoutParams(-2, -2)
    }
    override fun onDraw(canvas: Canvas) { super.onDraw(canvas); canvas.drawRect(0f, 0f, width.toFloat(), 2f, line) }
}
