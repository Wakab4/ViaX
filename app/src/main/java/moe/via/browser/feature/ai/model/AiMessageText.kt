package moe.via.browser.feature.ai.model

object AiMessageText {
    fun transcript(messages: List<AiMessage>): String = buildString {
        messages.forEach { message ->
            if (message.role == AiMessage.USER) append("## ")
            else message.reasoning?.takeIf(String::isNotEmpty)?.let { append("> "); append(it.replace("\n", "\n> ")) }
            append(message.content); append("\n\n")
        }
    }

    fun markdown(message: AiMessage): String = buildString {
        message.reasoning?.takeIf(String::isNotEmpty)?.let { reasoning ->
            append(">"); append(reasoning.replace("\n", "\n>")); if (message.content.isNotEmpty()) append("\n\n")
        }
        append(message.content)
    }
}
