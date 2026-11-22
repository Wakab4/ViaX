package moe.via.browser.feature.ai.text

import moe.via.browser.feature.ai.model.*

/** e0.Z/W render during IO conversion; Recycler rows only receive the resulting CharSequence. */
internal class AiMessageRenderer(private val engine: AiMarkdownEngine) {
    fun text(message: AiMessage) = engine.render(if (message.role == AiMessage.USER) message.content else AiMessageText.markdown(message))
    fun display(message: AiMessage): AiMessageDisplay = if (message.role == AiMessage.USER) AiUserMessageDisplay(message.id, text(message))
        else AiAssistantMessageDisplay(message.id, text(message))
}
