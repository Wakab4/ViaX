package moe.via.browser.feature.passwords.ui

import moe.via.browser.ui.theme.fonts.NativeTypography

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import moe.via.browser.R
import moe.via.browser.feature.passwords.model.PasswordEntry
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** Metadata-only candidates. The controller owns decrypted details and sensitive clipboard operations. */
class PasswordCandidateBar(private val activity: Activity, private val actions: Actions) {
    interface Actions {
        fun candidate(id: String)
        fun picker()
        fun manage()
        fun copyUsername()
        fun copyPassword()
        fun closeDetail()
    }
    private var entries = emptyList<PasswordEntry>()
    private var detail = false
    private var disposed = false
    private val items = LinearLayout(activity).apply { gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL }
    private val scroll = HorizontalScrollView(activity).apply {
        isHorizontalScrollBarEnabled = false
        clipToPadding = false
        setPadding(activity.dp(12), 0, activity.dp(12), 0)
        addView(items, FrameLayout.LayoutParams(-2, -1))
    }
    private val picker = ImageView(activity).apply {
        setImageResource(R.drawable.ref_password_save)
        imageTintList = ColorStateList.valueOf(activity.palette.icon)
        setPadding(activity.dp(13), activity.dp(4), activity.dp(13), activity.dp(4))
        setBackgroundResource(R.drawable.ref_search_round_feedback)
        contentDescription = "保存的密码"
        setOnClickListener {
            if (detail) { clearDetail(); actions.closeDetail() } else actions.picker()
        }
        setOnLongClickListener { actions.manage(); true }
    }
    val view: View = FrameLayout(activity).apply {
        isSaveEnabled = false
        setBackgroundColor(activity.palette.panel)
        addView(scroll, FrameLayout.LayoutParams(-1, -1).apply { marginEnd = activity.dp(72) })
        addView(picker, FrameLayout.LayoutParams(activity.dp(48), -1, Gravity.END).apply {
            marginEnd = activity.dp(12); topMargin = activity.dp(5); bottomMargin = activity.dp(5)
        })
        layoutParams = android.view.ViewGroup.LayoutParams(-1, activity.dp(40))
    }

    fun render(entries: List<PasswordEntry>) {
        if (disposed) return
        this.entries = entries.toList()
        clearDetail()
    }
    fun showDetail(username: String, password: String) {
        if (disposed) return
        detail = true
        picker.setImageResource(R.drawable.ref_ic_close); picker.contentDescription = "关闭"
        items.removeAllViews()
        add(username, 200, copy = true, clicked = actions::copyUsername)
        // Keep only the mask in this view; the plaintext argument is never stored in UI state.
        add("*".repeat(password.length), 200, copy = true, clicked = actions::copyPassword)
        scroll.scrollTo(0, 0)
    }
    fun clearDetail() {
        if (disposed) return
        detail = false
        picker.setImageResource(R.drawable.ref_password_save); picker.contentDescription = "保存的密码"
        items.removeAllViews()
        val maximum = if (entries.size >= 3) 120 else 200
        entries.forEach { entry -> add(entry.username, maximum) { actions.candidate(entry.id) } }
        scroll.scrollTo(0, 0)
    }
    fun dispose() {
        disposed = true; entries = emptyList(); detail = false
        items.removeAllViews(); picker.setOnClickListener(null); picker.setOnLongClickListener(null)
    }

    fun refreshTheme() {
        if (disposed) return
        view.setBackgroundColor(activity.palette.panel)
        picker.imageTintList = ColorStateList.valueOf(activity.palette.icon)
        for (index in 0 until items.childCount) {
            (items.getChildAt(index) as? TextView)?.apply {
                setTextColor(activity.palette.text)
                compoundDrawablesRelative.filterNotNull().forEach { it.setTint(activity.palette.icon) }
            }
        }
    }

    private fun add(value: String, maximum: Int, copy: Boolean = false, clicked: () -> Unit) {
        items.addView(NativeTypography.text(activity).apply {
            text = value; textSize = 14f; typeface = Typeface.DEFAULT
            setTextColor(activity.palette.text)
            gravity = Gravity.CENTER; setSingleLine(true); ellipsize = TextUtils.TruncateAt.END
            textDirection = View.TEXT_DIRECTION_LOCALE
            maxWidth = activity.dp(maximum)
            setPadding(activity.dp(8), 0, activity.dp(8), 0)
            isSaveEnabled = false
            setBackgroundResource(R.drawable.ref_search_round_feedback)
            if (copy) {
                val drawable = activity.getDrawable(R.drawable.ref_translation_copy)?.mutate()?.apply {
                    setTint(activity.palette.icon); setBounds(0, 0, activity.dp(18), activity.dp(18))
                }
                setCompoundDrawablesRelative(null, null, drawable, null)
                compoundDrawablePadding = activity.dp(2)
                contentDescription = if (items.childCount == 0) "复制用户名" else "复制密码"
            }
            setOnClickListener { clicked() }
        }, LinearLayout.LayoutParams(-2, -1).apply {
            setMargins(activity.dp(6), activity.dp(4), activity.dp(6), activity.dp(4))
        })
    }
}
