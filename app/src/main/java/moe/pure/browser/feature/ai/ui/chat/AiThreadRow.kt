package moe.pure.browser.feature.ai.ui.chat

import android.content.Context
import android.text.TextUtils
import android.text.format.DateUtils
import android.widget.RelativeLayout
import moe.pure.browser.R
import moe.pure.browser.feature.ai.model.AiThread
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

/** c9.y: title before the date, preview below, and a full rectangular selection background. */
internal class AiThreadRow(context: Context) : RelativeLayout(context) {
    private val title = context.chatText().apply {
        id = R.id.ai_chat_thread_title
        setSingleLine(); ellipsize = TextUtils.TruncateAt.END
    }
    private val time = context.chatText(size = 11f).apply {
        id = R.id.ai_chat_thread_time; setTextColor(context.palette.secondary)
    }
    private val preview = context.chatText(size = 12f).apply {
        setSingleLine(); ellipsize = TextUtils.TruncateAt.END; setTextColor(context.palette.secondary)
    }
    init {
        addView(context.chatIcon(R.drawable.ref_ai_chat_thread, "") {}.apply {
            id = R.id.ai_chat_thread_icon; isClickable = false; isFocusable = false; background = null
            setPadding(dp(13), 0, dp(13), 0)
        }, LayoutParams(dp(48), dp(48)).apply { marginStart = dp(3); addRule(CENTER_VERTICAL) })
        addView(title, LayoutParams(-1, -2).apply {
            addRule(END_OF, R.id.ai_chat_thread_icon); addRule(START_OF, R.id.ai_chat_thread_time)
            topMargin = dp(16); bottomMargin = dp(2)
        })
        addView(time, LayoutParams(-2, -2).apply {
            addRule(ALIGN_PARENT_END); addRule(ALIGN_TOP, R.id.ai_chat_thread_title)
            marginStart = dp(2); marginEnd = dp(16)
        })
        addView(preview, LayoutParams(-1, -2).apply {
            addRule(BELOW, R.id.ai_chat_thread_title); addRule(ALIGN_START, R.id.ai_chat_thread_title)
            bottomMargin = dp(16); marginEnd = dp(16)
        })
    }
    fun bind(value: AiThread, selected: Boolean) {
        title.text = value.name?.takeIf(String::isNotEmpty) ?: context.getString(R.string.ref_ai_dialog_untitled)
        preview.text = value.preview?.takeIf(String::isNotEmpty) ?: context.getString(R.string.ref_ai_dialog_no_messages)
        val now = System.currentTimeMillis()
        time.text = if (((now - value.updatedAt) / 1000L / 60L).toInt() < 1) context.getString(R.string.ref_ai_dialog_just_now)
        else DateUtils.getRelativeTimeSpanString(value.updatedAt, System.currentTimeMillis(), 60000L, 65552)
        select(selected)
    }
    fun select(selected: Boolean) { if (selected) setBackgroundColor(0x30808080) else setBackgroundResource(R.drawable.ref_ai_chat_row_feedback) }
}
