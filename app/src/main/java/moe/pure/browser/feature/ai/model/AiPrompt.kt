package moe.pure.browser.feature.ai.model

import java.util.UUID

data class AiPrompt(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val content: String = "",
    val type: Int = TYPE_SYSTEM,
    val required: List<String> = emptyList(),
    val flags: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
) {
    val requiresPage: Boolean get() = flags and REQUIRES_PAGE != 0

    companion object {
        const val TYPE_SYSTEM = 1
        const val TYPE_MESSAGE = 2
        const val REQUIRES_PAGE = 1
        const val DEFAULT_ID = "default"
    }
}
