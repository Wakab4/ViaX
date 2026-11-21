package moe.pure.browser.feature.ai.ui.chat

import android.content.Context
import android.graphics.Color
import android.view.ActionMode
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import moe.pure.browser.feature.ai.text.table.AiTableMovementMethod
import moe.pure.browser.R
import moe.pure.browser.feature.ai.model.AiMessageDisplay
import moe.pure.browser.feature.ai.text.AiSpannableFactory
import moe.pure.browser.feature.library.ui.recycler.bindNativeItemClick
import moe.pure.browser.ui.components.dp
import moe.pure.browser.ui.theme.palette

internal interface AiMessageRow { fun bind(value: AiMessageDisplay); fun updateText(value: AiMessageDisplay) }

private class MessageLabel(context: Context, private val action: (AiMessageDisplay, Int, View) -> Unit, user: Boolean) {
    private var message: AiMessageDisplay? = null
    val view = context.chatText().apply {
        if (user) setTextColor(Color.WHITE)
        setSpannableFactory(AiSpannableFactory); setTextIsSelectable(true); movementMethod = AiTableMovementMethod()
        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                var order = 0
                for (index in 0 until menu.size()) if (menu.getItem(index).itemId == android.R.id.copy) order = index
                menu.add(0, AiMessageAdapter.COPY, order, context.getString(R.string.ref_ai_dialog_copy_all))
                menu.add(0, AiMessageAdapter.PREVIEW, order, context.getString(R.string.ref_ai_dialog_preview))
                menu.add(0, AiMessageAdapter.DELETE, order, context.getString(R.string.ref_ai_dialog_delete))
                return true
            }
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                menu.removeItem(android.R.id.selectAll); menu.removeItem(android.R.id.cut); menu.removeItem(android.R.id.shareText); return true
            }
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                if (item.itemId !in listOf(AiMessageAdapter.COPY, AiMessageAdapter.PREVIEW, AiMessageAdapter.DELETE)) return false
                message?.let { action(it, item.itemId, this@apply) }; mode.finish(); return true
            }
            override fun onDestroyActionMode(mode: ActionMode) = Unit
        }
    }
    fun bind(value: AiMessageDisplay) { message = value; updateText(value) }
    fun updateText(value: AiMessageDisplay) { view.text = value.text }
}

/** c9.g0 is a padded FrameLayout with an end-aligned accent bubble and a 32dp start margin. */
internal class AiUserMessageRow(context: Context, action: (AiMessageDisplay, Int, View) -> Unit) : FrameLayout(context), AiMessageRow {
    private val label = MessageLabel(context, action, true)
    init {
        setPadding(dp(16), dp(16), dp(16), dp(16))
        addView(label.view.apply {
            setPadding(dp(8), dp(8), dp(8), dp(8)); background = context.chatSurface(context.palette.accent)
        }, LayoutParams(-2, -2, Gravity.END).apply { marginStart = dp(32) })
    }
    override fun bind(value: AiMessageDisplay) = label.bind(value)
    override fun updateText(value: AiMessageDisplay) = label.updateText(value)
}

/** c9.l keeps the assistant heading outside a separately pressed, rounded message container. */
internal class AiAssistantMessageRow(context: Context, private val action: (AiMessageDisplay, Int, View) -> Unit) : LinearLayout(context), AiMessageRow {
    private val label = MessageLabel(context, action, false)
    private val bubble = FrameLayout(context)
    init {
        orientation = VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16))
        addView(context.chatText(context.getString(R.string.ref_ai_message_assistant), 14f, true).apply {
            isAllCaps = true; setPadding(0, 0, 0, dp(4))
        }, LayoutParams(-1, -2))
        bubble.setPadding(dp(8), dp(8), dp(8), dp(8))
        bubble.background = context.chatPressedSurface(0x15808080, 0x30808080)
        bubble.addView(label.view, FrameLayout.LayoutParams(-2, -2)); addView(bubble, LayoutParams(-2, -2))
    }
    override fun bind(value: AiMessageDisplay) {
        label.bind(value)
        bubble.bindNativeItemClick { }
        bubble.setOnLongClickListener { action(value, AiMessageAdapter.MENU, bubble); true }
    }
    override fun updateText(value: AiMessageDisplay) = label.updateText(value)
}
