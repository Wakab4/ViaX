package moe.via.browser.feature.ai.ui.chat

import android.content.Context
import android.content.res.ColorStateList
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import moe.via.browser.R
import moe.via.browser.feature.ai.model.AiPageAttachment
import moe.via.browser.feature.library.data.NativeFaviconCache
import moe.via.browser.ui.theme.skin.NativeSkinIcons
import moe.via.browser.ui.components.dp
import moe.via.browser.ui.theme.palette

/** c9.e's webpage attachment; its raw URL reads x0 rather than using a resident WebView icon. */
internal class AiAttachmentChip(context: Context, onRemove: () -> Unit) : LinearLayout(context), AutoCloseable {
    private var closed = false
    private var previousClose: Long? = null
    private var remove: (() -> Unit)? = onRemove
    private val page = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        setPadding(dp(9), dp(9), dp(9), dp(9))
    }
    private val title = context.chatText(size = 12f).apply {
        setSingleLine(); ellipsize = TextUtils.TruncateAt.END
        minWidth = dp(56); maxWidth = dp(120)
    }
    private val clear = context.chatIcon(R.drawable.ref_ic_close, "移除网页", ::removeClicked).apply {
        setPadding(dp(9), dp(9), dp(9), dp(9))
        setBackgroundResource(R.drawable.ref_ai_chat_circle_feedback)
    }
    private val skin = NativeSkinIcons(context)

    init {
        orientation = HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        background = context.chatSurface(0x20808080, 18)
        addView(page, LayoutParams(dp(36), dp(36)))
        addView(title, LayoutParams(-2, -2))
        addView(clear, LayoutParams(dp(36), dp(36)))
        clearIcon()
    }

    fun bind(attachment: AiPageAttachment) {
        if (closed) return
        title.text = attachment.title
        previousClose = null
        val bitmap = attachment.url?.let { NativeFaviconCache.bitmap(context, it) }
        if (bitmap != null) {
            page.imageTintList = null
            page.setImageBitmap(bitmap)
        } else fallbackIcon()
    }

    private fun fallbackIcon() {
        page.imageTintList = ColorStateList.valueOf(context.palette.icon)
        page.setImageDrawable((skin.resolve(R.drawable.ref_ic_webpage)
            ?: context.getDrawable(R.drawable.ref_ic_webpage))?.mutate())
    }

    private fun clearIcon() {
        clear.imageTintList = ColorStateList.valueOf(context.palette.icon)
        clear.setImageDrawable((skin.resolve(R.drawable.ref_ic_close)
            ?: context.getDrawable(R.drawable.ref_ic_close))?.mutate())
    }

    private fun removeClicked() {
        if (closed) return
        val now = SystemClock.elapsedRealtime()
        val previous = previousClose
        if (previous == null || kotlin.math.abs(now - previous) > 300L) {
            previousClose = now
            remove?.invoke()
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        remove = null; previousClose = null
        clear.setOnClickListener(null)
        page.setImageDrawable(null); clear.setImageDrawable(null)
        title.text = ""
        // x0 owns the bitmap: dropping this view never recycles it or evicts the shared cache.
    }
}
