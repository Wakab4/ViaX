package moe.pure.browser.feature.ai.ui.chat

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.R
import moe.pure.browser.feature.ai.model.AiThread
import moe.pure.browser.feature.ai.model.AiMessageDisplay
import moe.pure.browser.feature.ai.model.AiPageAttachment
import moe.pure.browser.feature.ai.model.AiPromptChoice
import moe.pure.browser.feature.fonts.ui.NativeTypography
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.components.recycler.NativeRecyclerEdgeEffect
import moe.pure.browser.ui.theme.palette

internal class AiChatPage(context: Context, history: () -> Unit,
    private val more: (View) -> Unit, private val send: (String) -> Boolean, private val stop: () -> Unit,
    selectPrompt: (String) -> Unit, removeAttachment: () -> Unit,
    messageAction: (AiMessageDisplay, Int, View) -> Unit, private val isGenerating: () -> Boolean) : RelativeLayout(context) {
    private val title = context.chatText(context.getString(R.string.ref_ai_dialog_new_topic), 16f, true).apply { chatTitle() }
    private val list = RecyclerView(context).apply {
        id = R.id.ai_chat_messages_list
        layoutManager = object : LinearLayoutManager(context) {
            override fun requestChildRectangleOnScreen(parent: RecyclerView, child: View, rect: Rect, immediate: Boolean) = false
            override fun requestChildRectangleOnScreen(parent: RecyclerView, child: View, rect: Rect, immediate: Boolean, focusedChildVisible: Boolean) = false
        }.apply { stackFromEnd = true }
        NativeRecyclerEdgeEffect.install(this)
    }
    private val messages = AiMessageAdapter(context, messageAction)
    private val prompts = AiPromptStrip(context, selectPrompt, removeAttachment).apply { id = R.id.ai_chat_prompt_list }
    private val empty = context.chatText("¯\\_(ツ)_/¯", 22f).apply {
        setTextColor(context.palette.secondary); contentDescription = context.getString(R.string.ref_ai_dialog_missing_message)
    }
    private val input = NativeTypography.edit(context)
    private val submit = context.chatIcon(R.drawable.ref_ai_chat_send, context.getString(R.string.ref_ai_dialog_send), ::sendOrStop)
    init {
        val toolbar = LinearLayout(context).apply { id = R.id.ai_chat_toolbar; orientation = LinearLayout.HORIZONTAL }
        toolbar.addView(context.chatIcon(R.drawable.ref_ai_chat_threads, context.getString(R.string.ref_ai_dialog_all_topics), history).apply {
            setPadding(0, dp(13), 0, dp(13))
        }, LinearLayout.LayoutParams(dp(54), dp(48)).apply { marginStart = dp(3) })
        toolbar.addView(title, LinearLayout.LayoutParams(0, -2, 1f).apply { gravity = Gravity.CENTER_VERTICAL; marginEnd = dp(16) })
        val overflow = context.chatIcon(R.drawable.ref_ai_chat_more, context.getString(R.string.ref_closed_tabs_more)) {}
        overflow.setOnClickListener { more(overflow) }
        toolbar.addView(overflow, LinearLayout.LayoutParams(dp(54), dp(48)).apply { marginEnd = dp(3) })
        addView(toolbar, LayoutParams(-1, -2).apply { topMargin = dp(7) })
        list.adapter = messages
        addView(list, LayoutParams(-1, -1).apply { addRule(BELOW, R.id.ai_chat_toolbar); addRule(ABOVE, R.id.ai_chat_prompt_list) })
        addView(empty, LayoutParams(-2, -2).apply { addRule(CENTER_IN_PARENT) })
        addView(prompts, LayoutParams(-1, dp(32)).apply { addRule(ABOVE, R.id.ai_chat_input_row); topMargin = dp(8) })
        val inputRow = LinearLayout(context).apply {
            id = R.id.ai_chat_input_row; orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setStroke(dp(2), 0x30808080) }
        }
        input.apply {
            if (Build.VERSION.SDK_INT >= 35) setLocalePreferredLineHeightForMinimumUsed(false)
            setBackgroundColor(Color.TRANSPARENT); setPadding(dp(12), dp(9), dp(12), dp(9))
            chatTextSize(14f); setTextColor(context.palette.text); setHintTextColor(context.palette.hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 1; maxLines = 5; gravity = Gravity.TOP; setHint(R.string.ref_ai_dialog_message)
            setOnFocusChangeListener { _, focused -> submit.visibility = if (focused || isGenerating()) VISIBLE else GONE }
        }
        submit.apply { setPadding(dp(6), 0, dp(6), 0); imageTintList = ColorStateList.valueOf(Color.WHITE); visibility = GONE }
        inputRow.addView(input, LinearLayout.LayoutParams(0, -2, 1f).apply { gravity = Gravity.CENTER_VERTICAL })
        inputRow.addView(submit, LinearLayout.LayoutParams(dp(32), dp(32)).apply { gravity = Gravity.BOTTOM; marginEnd = dp(4); topMargin = dp(4); bottomMargin = dp(4) })
        addView(inputRow, LayoutParams(-1, -2).apply { addRule(ALIGN_PARENT_BOTTOM); marginStart = dp(16); marginEnd = dp(16); topMargin = dp(8); bottomMargin = dp(8) })
        renderSubmit(false)
    }
    fun showTopic(topic: AiThread?) { title.text = topic?.name?.takeIf(String::isNotEmpty) ?: context.getString(R.string.ref_ai_dialog_new_topic) }
    fun titleText() = title.text.toString()
    fun showBusy(busy: Boolean) { renderSubmit(busy); submit.visibility = if (busy || input.hasFocus()) VISIBLE else GONE }
    fun submitMessages(values: List<AiMessageDisplay>) { messages.submit(values, list); empty.visibility = if (values.isEmpty()) VISIBLE else GONE }
    fun updateMessage(position: Int) { messages.notifyItemChanged(position, "") }
    fun submitPrompts(attachment: AiPageAttachment?, values: List<AiPromptChoice>) { prompts.submit(if (messages.itemCount <= 0) attachment else null, values, messages.generation) }
    private fun renderSubmit(busy: Boolean) {
        submit.chatImage(if (busy) R.drawable.ref_ai_chat_stop else R.drawable.ref_ai_chat_send)
        submit.contentDescription = context.getString(if (busy) R.string.ref_ai_dialog_stop else R.string.ref_ai_dialog_send)
        val color = if (busy) 0xffd64146.toInt() else context.palette.accent
        val pressed = if (busy) color or Int.MIN_VALUE else if (context.palette.night) 0xff48598b.toInt() else 0xff5b73b5.toInt()
        submit.background = context.chatPressedSurface(color, pressed, oval = true)
    }
    fun hideKeyboard() { context.getSystemService(android.view.inputmethod.InputMethodManager::class.java)?.hideSoftInputFromWindow(input.windowToken, 0) }
    private fun sendOrStop() { if (isGenerating()) stop() else { send(input.text.toString()); input.setText("") } }
    fun close() { prompts.close(); list.stopScroll(); list.itemAnimator?.endAnimations(); (list.edgeEffectFactory as? NativeRecyclerEdgeEffect)?.close(); list.adapter = null }
}
