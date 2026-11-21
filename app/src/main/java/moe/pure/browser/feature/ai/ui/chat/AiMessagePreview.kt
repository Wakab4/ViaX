package moe.pure.browser.feature.ai.ui.chat

import android.content.Context
import android.os.Build
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import moe.pure.browser.feature.ai.text.table.AiTableMovementMethod
import moe.pure.browser.R
import moe.pure.browser.ui.components.NativeToolbarTap
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.toolbar.NativePageToolbar
import moe.pure.browser.ui.components.toolbar.NativeToolbarAction
import moe.pure.browser.ui.theme.palette

/** q0 keeps its toolbar while loading/missing; only the other format's action is shown. */
internal class AiMessagePreview(context: Context, back: () -> Unit) : LinearLayout(context) {
    private val toolbar = NativePageToolbar(context, context.getString(R.string.ref_ai_dialog_message), back)
    private val scroll = ScrollView(context).apply {
        isFillViewport = true
        overScrollMode = if (Build.VERSION.SDK_INT >= 32) OVER_SCROLL_IF_CONTENT_SCROLLS else OVER_SCROLL_NEVER
    }
    private val text = context.chatText().apply {
        setPadding(dp(16), dp(16), dp(16), dp(16)); setTextIsSelectable(true)
        movementMethod = AiTableMovementMethod()
    }
    private var raw: String? = null
    private var formatted: CharSequence? = null
    init {
        orientation = VERTICAL; setBackgroundColor(context.palette.canvas)
        toolbar.addAction(NativeToolbarAction(R.id.ai_preview_markdown_action, 0, null, context.getString(R.string.ref_ai_dialog_markdown), false)) { display(true) }
        toolbar.addAction(NativeToolbarAction(R.id.ai_preview_plain_action, 0, null, context.getString(R.string.ref_ai_dialog_plain), false)) { display(false) }
        NativeToolbarTap.bind(toolbar) { scroll.smoothScrollTo(0, 0) }
        addView(toolbar, LayoutParams(-1, -2))
        scroll.addView(text, FrameLayout.LayoutParams(-1, -2)); addView(scroll, LayoutParams(-1, -1))
    }
    fun missing() { text.setText(R.string.ref_ai_dialog_missing_message) }
    fun show(raw: String, formatted: CharSequence?) { this.raw = raw; this.formatted = formatted; display(true) }
    private fun display(markdown: Boolean) {
        text.text = if (markdown) formatted else raw
        toolbar.setItemVisible(R.id.ai_preview_markdown_action, !markdown)
        toolbar.setItemVisible(R.id.ai_preview_plain_action, markdown)
    }
}
