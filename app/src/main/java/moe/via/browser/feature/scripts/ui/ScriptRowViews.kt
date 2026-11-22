package moe.via.browser.feature.scripts.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.components.touchFeedback
import moe.via.browser.ui.theme.palette
import moe.via.browser.ui.theme.skin.NativeSkinIcons

/** Script rows have an independent checkbox and a separately clickable label. */
internal object ScriptRowViews {
    fun label(activity: Activity, text: String, detail: Boolean = false) = NativeTypography.text(activity).apply {
        this.text = text
        setTextColor(if (detail) activity.palette.secondary else activity.palette.text)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimensionPixelSize(if (detail) R.dimen.settings_detail_size else R.dimen.dialog_body_size).toFloat())
        maxLines = if (detail) 3 else 1; ellipsize = android.text.TextUtils.TruncateAt.END
    }
    fun row(activity: Activity, title: String, detail: String? = null, checked: Boolean? = null, enabled: Boolean = true,
        click: () -> Unit, toggle: ((Boolean) -> Unit)? = null): View {
        val content = LinearLayout(activity).apply {
            gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(20), dp(16), dp(20))
            background = activity.touchFeedback(); isEnabled = enabled; setOnClickListener { click() }
        }
        val labels = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        labels.addView(label(activity, title), LinearLayout.LayoutParams(-1, -2))
        if (!detail.isNullOrEmpty()) labels.addView(label(activity, detail, true), LinearLayout.LayoutParams(if (checked == null) -2 else -1, -2))
        content.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        if (checked != null) content.addView(CheckBox(activity).apply {
            isChecked = checked; buttonDrawable = activity.getDrawable(R.drawable.dialog_checkbox); buttonTintList = null
            minimumWidth = 0; minimumHeight = 0; setPadding(0, 0, 0, 0); background = null
            contentDescription = "$title ${if (checked) "已启用" else "已禁用"}"
            isEnabled = enabled; setOnCheckedChangeListener { _, value -> contentDescription = "$title ${if (value) "已启用" else "已禁用"}"; toggle?.invoke(value) }
        }, LinearLayout.LayoutParams(-2, activity.dp(20)).apply { marginStart = activity.dp(12) })
        if (!enabled) content.alpha = .5f
        return content
    }
    fun add(activity: Activity, title: String, click: () -> Unit) = label(activity, title).apply {
        val icon = (NativeSkinIcons(activity).resolve(R.drawable.ref_ic_add, "ic_add") ?: activity.getDrawable(R.drawable.ref_ic_add))!!.mutate().apply { setTint(activity.palette.text); setBounds(0, 0, activity.dp(18), activity.dp(18)) }
        setCompoundDrawables(icon, null, null, null); compoundDrawablePadding = dp(18)
        setPadding(dp(16), dp(20), dp(16), dp(20)); background = activity.touchFeedback(); setOnClickListener { click() }
    }
}
