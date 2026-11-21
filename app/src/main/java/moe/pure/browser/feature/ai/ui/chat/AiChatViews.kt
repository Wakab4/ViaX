package moe.pure.browser.feature.ai.ui.chat

import moe.pure.browser.feature.fonts.ui.NativeTypography

import moe.pure.browser.ui.message.BrowserMessages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import moe.pure.browser.R
import moe.pure.browser.feature.settings.skin.NativeSkinIcons
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

internal fun Context.chatText(value: String = "", size: Float = 14f, bold: Boolean = false) = NativeTypography.text(this).apply {
    text = value
    chatTextSize(size)
    setTextColor(palette.text)
    if (bold) NativeTypography.style(this, android.graphics.Typeface.BOLD)
}

internal fun Context.chatIcon(resource: Int, label: String, action: () -> Unit) = ImageView(this).apply {
    chatImage(resource)
    imageTintList = ColorStateList.valueOf(palette.icon)
    scaleType = ImageView.ScaleType.FIT_CENTER
    setPadding(dp(13), dp(13), dp(13), dp(13))
    setBackgroundResource(R.drawable.ref_ai_chat_feedback)
    contentDescription = label
    isFocusable = true
    setOnClickListener { action() }
}

internal fun ImageView.chatImage(resource: Int) {
    val key = when (resource) {
        R.drawable.ref_ai_chat_thread -> "ic_thread"
        R.drawable.ref_ai_chat_threads -> "ic_threads"
        R.drawable.ref_ai_chat_settings -> "ic_settings"
        R.drawable.ref_ai_chat_more -> "ic_overflow"
        R.drawable.ref_ai_chat_send -> "ic_send"
        R.drawable.ref_ai_chat_stop -> "ic_stop"
        else -> null
    }
    setImageDrawable(NativeSkinIcons(context).resolve(resource, key) ?: context.getDrawable(resource))
}

internal fun Context.chatPressedSurface(color: Int, pressed: Int, oval: Boolean = false) = StateListDrawable().apply {
    fun shape(value: Int) = chatSurface(value).apply { if (oval) shape = GradientDrawable.OVAL }
    addState(intArrayOf(android.R.attr.state_pressed), shape(pressed))
    addState(intArrayOf(), shape(color))
}

internal fun Context.chatSurface(color: Int, radius: Int = 18) = GradientDrawable().apply {
    setColor(color)
    cornerRadius = dp(radius).toFloat()
}

internal fun Context.chatPromptBackground() = StateListDrawable().apply {
    fun outline(color: Int, width: Int = 1) = chatSurface(android.graphics.Color.TRANSPARENT).apply { setStroke(dp(width), color) }
    addState(intArrayOf(-android.R.attr.state_pressed, -android.R.attr.state_selected, -android.R.attr.state_checked, -android.R.attr.state_focused), outline(0x30808080))
    addState(intArrayOf(-android.R.attr.state_pressed, android.R.attr.state_checked, -android.R.attr.state_focused), outline(palette.accent))
    addState(intArrayOf(-android.R.attr.state_pressed, android.R.attr.state_selected, -android.R.attr.state_focused), outline(palette.accent))
    addState(intArrayOf(android.R.attr.state_pressed), chatSurface(0x30808080))
    addState(intArrayOf(android.R.attr.state_focused), outline(0x60808080, 3))
}

internal fun Context.copyAiText(value: String) {
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, value))
    BrowserMessages.show(this, getString(R.string.ref_ai_dialog_copied))
}

internal fun TextView.chatTitle() {
    gravity = Gravity.CENTER_VERTICAL
    setSingleLine()
    ellipsize = android.text.TextUtils.TruncateAt.END
    chatTextSize(16f)
    NativeTypography.style(this, android.graphics.Typeface.BOLD)
}

internal fun TextView.chatTextSize(sp: Float) {
    val pixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    setTextSize(TypedValue.COMPLEX_UNIT_PX, (pixels + .5f).toInt().toFloat())
}
