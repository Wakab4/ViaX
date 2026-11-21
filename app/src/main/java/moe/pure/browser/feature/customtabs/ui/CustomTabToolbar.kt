package moe.pure.browser.feature.customtabs.ui

import moe.pure.browser.feature.fonts.ui.NativeTypography

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.feature.customtabs.CustomTabRequest
import moe.pure.browser.ui.components.DividerLayout
import moe.pure.browser.ui.components.SkinIconBinder
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.touchFeedback
import moe.pure.browser.ui.theme.palette

/** GP mode 4 uses a 48 dp z toolbar: close, bold host, caller action/share, overflow. */
internal class CustomTabToolbar(
    private val activity: Activity,
    request: CustomTabRequest,
    close: () -> Unit,
    action: () -> Unit,
    overflow: (View) -> Unit
) : AutoCloseable {
    private val icons = SkinIconBinder(activity)
    private val images = mutableListOf<ImageView>()
    private val captions = mutableListOf<TextView>()
    val root = DividerLayout(activity, atBottom = true).apply {
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = android.view.ViewGroup.LayoutParams(-1, activity.dp(48))
        setBackgroundColor(activity.palette.panel)
    }
    private val title = NativeTypography.text(activity).apply {
        text = "无标题"; textSize = 16f; NativeTypography.style(this, android.graphics.Typeface.BOLD)
        setTextColor(activity.palette.text); gravity = Gravity.CENTER_VERTICAL
        setSingleLine(); ellipsize = android.text.TextUtils.TruncateAt.END
    }
    init {
        val closeIcon = image("向上导航", close)
        request.closeIcon?.let { closeIcon.setImageDrawable(BitmapDrawable(activity.resources, it)) }
            ?: icons.image(closeIcon, R.drawable.ref_ic_close)
        root.addView(closeIcon, iconParams())
        root.addView(title, LinearLayout.LayoutParams(0, -2, 24f).apply { gravity = Gravity.CENTER_VERTICAL })
        captions += title
        root.addView(Space(activity), LinearLayout.LayoutParams(0, -1, 1f))
        val caller = request.action
        if (caller == null) {
            root.addView(image("分享", action).also { icons.image(it, R.drawable.ref_ic_share) }, iconParams())
        } else if (caller.icon != null) {
            root.addView(image(caller.description.orEmpty(), action).apply { setImageDrawable(BitmapDrawable(activity.resources, caller.icon)) }, iconParams())
        } else {
            root.addView(NativeTypography.text(activity).apply {
                text = caller.description; contentDescription = caller.description
                textSize = 14f; setTextColor(activity.palette.text); setSingleLine(); isAllCaps = true
                gravity = Gravity.CENTER; setPadding(dp(13), 0, dp(13), 0); background = activity.touchFeedback()
                setOnClickListener { action() }; captions += this
            }, LinearLayout.LayoutParams(-2, -1).apply { val margin = activity.dp(1); setMargins(margin, margin, margin, margin) })
        }
        root.addView(image("更多") { overflow(root.getChildAt(root.childCount - 1)) }.also {
            icons.image(it, R.drawable.ref_ic_overflow)
        }, iconParams())
    }
    fun page(url: String) {
        val scheme = url.indexOf("://")
        title.text = if (scheme < 0) "" else {
            val start = scheme + 3
            val end = url.indexOf('/', start).takeIf { it >= 0 } ?: url.length
            url.substring(start, end).substringBefore(':')
        }
    }
    fun recolor(background: Int?, foreground: Int?) {
        root.setBackgroundColor(background ?: activity.palette.panel)
        val icon = foreground ?: activity.palette.icon
        images.forEach { it.imageTintList = ColorStateList.valueOf(icon) }
        val text = if (foreground == null) activity.palette.text else if (foreground == 0xFF232323.toInt()) 0xDE000000.toInt() else foreground
        captions.forEach { it.setTextColor(text) }
    }
    private fun image(label: String, clicked: () -> Unit) = ImageView(activity).apply {
        contentDescription = label; imageTintList = ColorStateList.valueOf(activity.palette.icon)
        setPadding(dp(13), 0, dp(13), 0); background = activity.touchFeedback()
        setOnClickListener { clicked() }; images += this
    }
    private fun iconParams() = LinearLayout.LayoutParams(activity.dp(48), -1).apply {
        val margin = activity.dp(1); setMargins(margin, margin, margin, margin)
    }
    override fun close() = icons.close()
}
