package moe.via.browser.browser.ui.engine

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette
import kotlin.math.roundToInt

/** Native ra.h: the description is centered; the title and two actions align to that description. */
internal class EngineFailurePage(context: Context, private val primaryAction: () -> Unit, exitAction: () -> Unit) : RelativeLayout(context) {
    private val description = text(16).apply {
        minWidth = dp(120); maxWidth = dp(300)
    }
    private val title = text(22).apply {
        text = context.getString(R.string.ref_engine_title)
        NativeTypography.style(this, android.graphics.Typeface.BOLD)
    }
    private val primary = text(16).apply {
        setPadding(dp(12), dp(12), dp(12), dp(12))
        setTextColor(Color.WHITE)
        background = RippleDrawable(ColorStateList.valueOf(if (context.palette.night) 0xFF48598B.toInt() else 0xFF5B73B5.toInt()),
            rounded(context.palette.accent), rounded(Color.BLACK))
        setOnClickListener { primaryAction() }
    }
    private val exit = text(16).apply {
        text = context.getString(R.string.ref_engine_exit)
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_enabled, android.R.attr.state_pressed), rounded(0x30808080))
            addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
        }
        setOnClickListener { exitAction() }
    }

    init {
        layoutParams = FrameLayout.LayoutParams(-1, -1)
        setBackgroundColor(context.palette.canvas)
        addView(title, alignedTo(description).apply {
            addRule(ABOVE, description.id); marginStart = dp(16); marginEnd = dp(16)
        })
        addView(description, LayoutParams(-2, -2).apply {
            addRule(CENTER_VERTICAL); addRule(CENTER_HORIZONTAL)
            marginStart = dp(32); marginEnd = dp(32); topMargin = dp(12); bottomMargin = dp(24)
        })
        addView(primary, alignedTo(description).apply { addRule(BELOW, description.id) })
        addView(exit, alignedTo(description).apply { addRule(BELOW, primary.id); topMargin = dp(8) })
    }

    fun render(code: Int) {
        description.setText(if (code == 1) R.string.ref_engine_missing else R.string.ref_engine_crash)
        primary.setText(if (code == 1) R.string.ref_engine_install else R.string.ref_engine_restart)
    }

    private fun text(sp: Int) = NativeTypography.text(context).apply {
        id = View.generateViewId()
        gravity = Gravity.CENTER
        setTextColor(context.palette.text)
        setTextSize(TypedValue.COMPLEX_UNIT_PX,
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), resources.displayMetrics).roundToInt().toFloat())
    }

    private fun alignedTo(view: View) = LayoutParams(-2, -2).apply {
        addRule(ALIGN_START, view.id); addRule(ALIGN_END, view.id)
    }

    private fun rounded(color: Int) = GradientDrawable().apply {
        cornerRadius = dp(18).toFloat(); setColor(color)
    }
}
