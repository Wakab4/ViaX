package moe.pure.browser.feature.ai.model

import java.util.UUID

data class AiBackend(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val baseUrl: String = "",
    val token: String = "",
    val models: List<String> = emptyList(),
    val flags: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
) {
    val firstModel: String? get() = models.firstOrNull()

    override fun toString(): String = "AiBackend(id=$id, name=$name, baseUrl=$baseUrl, models=$models, flags=$flags)"
}
