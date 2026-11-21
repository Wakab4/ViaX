package moe.pure.browser.ui.message

import android.graphics.drawable.Drawable

/** f6.h builder values; bottomMarginPx is physical, while zero selects the Native 120dp default. */
data class MessageContent(
    val text: String,
    val action: String? = null,
    val icon: Drawable? = null,
    val singleLine: Boolean = false,
    val clicked: (() -> Unit)? = null,
    val longClicked: (() -> Boolean)? = null,
    val cancelled: (() -> Unit)? = null,
    val bottomMarginPx: Int = 0,
    val timeoutMillis: Long? = null,
    val removed: (() -> Unit)? = null
)
