package moe.pure.browser.feature.search.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.feature.search.model.SearchEngine
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** The 40dp result-page engine strip. Its owner reserves page space and handles navigation. */
class SearchEngineToolbar(private val activity: Activity, private val callbacks: Callbacks) {
    interface Callbacks {
        fun onEngine(engine: SearchEngine): Boolean
        fun onSettings()
    }

    private val items = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }
    val view: View = HorizontalScrollView(activity).apply {
        isHorizontalScrollBarEnabled = false
        clipToPadding = false
        setPadding(dp(12), 0, dp(12), 0)
        addView(items, android.widget.FrameLayout.LayoutParams(-2, dp(40)))
        layoutParams = android.view.ViewGroup.LayoutParams(-1, dp(40))
        visibility = View.GONE
    }
    private var selected: Int? = null
    private var disposed = false

    fun render(
        engines: List<SearchEngine>, currentEngineId: Int?, backgroundColor: Int = Color.TRANSPARENT,
        iconColor: Int? = null, textColor: Int? = null
    ) {
        if (disposed) return
        selected = currentEngineId
        items.removeAllViews()
        view.setBackgroundColor(backgroundColor)
        view.visibility = if (engines.size > 1 && engines.any { it.id == currentEngineId }) View.VISIBLE else View.GONE
        val normal = textColor ?: activity.palette.text
        engines.forEach { engine ->
            items.addView(NativeTypography.text(activity).apply {
                text = engine.name
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,
                    resources.getDimensionPixelSize(R.dimen.settings_detail_size).toFloat())
                maxWidth = dp(96)
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                setPadding(dp(12), 0, dp(12), 0)
                isSelected = engine.id == selected
                setTextColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
                    intArrayOf(activity.palette.accent, normal)))
                background = chipBackground()
                tag = engine.id
                setOnClickListener {
                    if (engine.id != selected && callbacks.onEngine(engine)) {
                        selected = engine.id
                        for (index in 0 until items.childCount) {
                            val child = items.getChildAt(index)
                            child.isSelected = child.tag == selected
                        }
                    }
                }
            }, cell(-2))
        }
        items.addView(ImageView(activity).apply {
            setImageResource(R.drawable.ref_search_settings)
            imageTintList = ColorStateList.valueOf(iconColor ?: activity.palette.icon)
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setBackgroundResource(R.drawable.ref_search_round_feedback)
            contentDescription = "搜索设置"
            setOnClickListener { callbacks.onSettings() }
        }, cell(activity.dp(48)))
        items.post {
            val current = (0 until items.childCount).map(items::getChildAt).firstOrNull { it.tag == selected }
            current?.let { (view as HorizontalScrollView).smoothScrollTo((it.left - activity.dp(12)).coerceAtLeast(0), 0) }
        }
    }

    private fun cell(width: Int) = LinearLayout.LayoutParams(width, activity.dp(28)).apply {
        setMargins(activity.dp(4), activity.dp(6), activity.dp(4), activity.dp(6))
    }

    private fun chipBackground(): StateListDrawable {
        fun shape(fill: Int = Color.TRANSPARENT, stroke: Int = 0x30808080, width: Int = 1) = GradientDrawable().apply {
            cornerRadius = activity.dp(18).toFloat()
            setColor(fill)
            if (width > 0) setStroke(activity.dp(width), stroke)
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), shape(0x30808080, width = 0))
            addState(intArrayOf(android.R.attr.state_focused), shape(stroke = 0x60808080, width = 3))
            addState(intArrayOf(android.R.attr.state_selected), shape(stroke = activity.palette.accent))
            addState(intArrayOf(), shape())
        }
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        items.removeAllViews()
        (view.parent as? android.view.ViewGroup)?.removeView(view)
    }
}
