package moe.pure.browser.feature.media.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.ui.components.dp

/** Direct dimensions and stacking from GP res/layout/a4.xml. */
internal class MediaOverlayViews(private val context: Context, root: FrameLayout) {
    val gradient = View(context).apply {
        background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x90000000.toInt(), 0))
    }
    val title = NativeTypography.text(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setTextColor(Color.WHITE)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setSingleLine(true)
        ellipsize = TextUtils.TruncateAt.END
    }
    val hud = NativeTypography.text(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18f)
        NativeTypography.style(this, android.graphics.Typeface.BOLD)
        setTextColor(Color.WHITE)
        setPadding(dp(16), dp(16), dp(16), dp(16))
        setSingleLine(true)
        background = roundSurface(Int.MIN_VALUE, 18)
    }
    val controls = RelativeLayout(context)
    val lock = button(R.drawable.ref_media_unlock, "锁定")
    val orientation = button(R.drawable.ref_media_orientation, "屏幕方向")
    val pip = button(R.drawable.ref_media_pip, "画中画")
    val speedLabel = NativeTypography.text(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f)
        NativeTypography.style(this, android.graphics.Typeface.BOLD)
        setTextColor(Color.WHITE)
        text = "1.0X"
    }
    val speed = FrameLayout(context).apply {
        id = View.generateViewId()
        background = buttonSurface()
        addView(speedLabel, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
    }

    init {
        root.addView(gradient, FrameLayout.LayoutParams(-1, context.dp(32)))
        root.addView(title, FrameLayout.LayoutParams(-1, -2).apply { marginEnd = context.dp(56) })
        root.addView(hud, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        root.addView(controls, FrameLayout.LayoutParams(-1, -1))
        controls.addView(lock, buttonParams().apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            addRule(RelativeLayout.CENTER_VERTICAL)
        })
        controls.addView(speed, buttonParams().apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            addRule(RelativeLayout.ABOVE, lock.id)
        })
        controls.addView(orientation, buttonParams().apply {
            addRule(RelativeLayout.ALIGN_PARENT_END)
            addRule(RelativeLayout.BELOW, lock.id)
        })
        controls.addView(pip, RelativeLayout.LayoutParams(context.dp(40), context.dp(40)).apply {
            marginStart = context.dp(16); marginEnd = context.dp(16)
            addRule(RelativeLayout.ALIGN_PARENT_START)
            addRule(RelativeLayout.ALIGN_TOP, lock.id)
        })
        all.forEach { it.visibility = View.GONE }
    }

    val all get() = listOf(title, gradient, speed, orientation, lock, pip, hud)
    val toolbar get() = listOf(title, gradient, speed, orientation, lock, pip)

    private fun button(icon: Int, caption: String) = ImageView(context).apply {
        id = View.generateViewId()
        setImageResource(icon)
        imageTintList = ColorStateList.valueOf(Color.WHITE)
        setPadding(dp(10), dp(10), dp(10), dp(10))
        background = buttonSurface()
        contentDescription = caption
    }

    private fun buttonParams() = RelativeLayout.LayoutParams(context.dp(40), context.dp(40)).apply {
        marginStart = context.dp(16); marginEnd = context.dp(16)
        topMargin = context.dp(8); bottomMargin = context.dp(8)
    }

    private fun roundSurface(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = context.dp(radius).toFloat()
    }

    private fun buttonSurface() = RippleDrawable(
        ColorStateList.valueOf(0x66000000), roundSurface(0x33000000, 24), roundSurface(Color.BLACK, 24),
    )
}
