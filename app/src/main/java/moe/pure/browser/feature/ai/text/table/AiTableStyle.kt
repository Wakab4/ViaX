package moe.pure.browser.feature.ai.text.table

import android.content.Context
import moe.pure.browser.ui.components.dp

/** pa.c/o6.e configure the short display dimension, not the current dialog's width. */
internal class AiTableStyle(context: Context) {
    val padding = context.dp(4)
    val borderWidth = context.dp(1)
    val borderColor = 0x40808080
    val headerBackground = 0
    val evenBackground = 0
    val oddBackground = 1
    val initialWidth = maxOf(0, minOf(context.resources.displayMetrics.widthPixels, context.resources.displayMetrics.heightPixels) - context.dp(64))
}

internal data class AiTableCell(val alignment: Int, val text: CharSequence)
internal class AiTableBlockSpan
