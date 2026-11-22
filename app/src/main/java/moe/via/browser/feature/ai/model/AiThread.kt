package moe.via.browser.feature.ai.model

import java.util.UUID

data class AiThread(
    val id: String = UUID.randomUUID().toString(),
    var name: String? = null,
    var preview: String? = null,
    val flags: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = createdAt
)
