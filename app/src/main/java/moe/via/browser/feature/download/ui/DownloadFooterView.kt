package moe.via.browser.feature.download.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette

/** b1.f3/u4 + a0: independent left actions and a right edit/done action. */
internal class DownloadFooterView(context: Context, private val state: DownloadPanelState,
    actions: DownloadManagerPage.Actions) : LinearLayout(context) {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x30808080 }
    private val create = button(R.string.ref_download_ui_new, actions::create)
    private val all = button(R.string.ref_download_ui_select_all, actions::selectAll)
    private val remove = button(R.string.ref_download_ui_delete, actions::removeSelected, true)
    private val edit = button(R.string.ref_download_ui_edit, actions::toggleEditing)

    init {
        gravity = Gravity.CENTER_VERTICAL
        setWillNotDraw(false)
        addView(create); addView(all); addView(remove)
        addView(Space(context), LayoutParams(0, context.dp(48), 1f))
        addView(edit)
    }

    fun render(count: Int) {
        create.visibility = if (state.editing) GONE else VISIBLE
        all.visibility = if (state.editing) VISIBLE else GONE
        remove.visibility = if (state.editing) VISIBLE else GONE
        val selected = state.selected.size
        all.setText(if (count > 0 && selected == count) R.string.ref_download_ui_cancel_all else R.string.ref_download_ui_select_all)
        remove.text = if (selected > 0) context.getString(R.string.ref_download_ui_delete_count, selected)
            else context.getString(R.string.ref_download_ui_delete)
        remove.isEnabled = selected > 0; remove.isSelected = selected > 0
        edit.setText(if (state.editing) R.string.ref_download_ui_done else R.string.ref_download_ui_edit)
        edit.isEnabled = count > 0 || state.editing
    }

    private fun button(label: Int, action: () -> Unit, destructive: Boolean = false) = NativeTypography.text(context).apply {
        setText(label); textSize = 14f; gravity = Gravity.CENTER
        val color = context.palette.text
        setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_enabled, android.R.attr.state_selected), intArrayOf()),
            intArrayOf((color and 0xffffff) or 0x80000000.toInt(),
                if (destructive) 0xffd64146.toInt() else color, color)))
        setPadding(dp(16), 0, dp(16), 0); minHeight = dp(48)
        background = context.touchFeedback(18)
        setOnClickListener { action() }
        layoutParams = LayoutParams(-2, -2)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawRect(0f, 0f, width.toFloat(), 2f, line)
    }
}
