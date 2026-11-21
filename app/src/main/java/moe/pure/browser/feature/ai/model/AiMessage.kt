package moe.pure.browser.feature.ai.model

import java.util.UUID

data class AiMessage(
    val id: String = UUID.randomUUID().toString(),
    val threadId: String,
    val role: String = USER,
    val content: String = "",
    val reasoning: String? = null,
    val errorFlags: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isError: Boolean get() = errorFlags and ERROR != 0

    companion object {
        const val SYSTEM = "system"
        const val USER = "user"
        const val ASSISTANT = "assistant"
        const val ERROR = 1
    }
}
