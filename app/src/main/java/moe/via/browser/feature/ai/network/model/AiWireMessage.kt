package moe.via.browser.feature.ai.network.model

/** p9.e/i/j are nullable, mutable wire objects; tool deltas append into their function fields. */
data class AiWireMessage(var content: String? = null, var role: String? = null, var reasoning: String? = null,
    var toolCalls: MutableList<AiToolCall>? = null, var toolCallId: String? = null)
data class AiToolCall(var id: String? = null, var function: AiToolFunction? = null, var type: String? = null, var index: Int = 0)
data class AiToolFunction(var name: String? = null, var arguments: String? = null)

internal data class AiChoice(val index: Int, val message: AiWireMessage?, val finishReason: String?)
internal data class AiDeltaChoice(val index: Int, val message: AiWireMessage, val finishReason: String?)
internal data class AiUsage(val prompt: Int, val completion: Int, val total: Int)
internal data class AiCompletionEnvelope(val id: String?, val created: Int, val model: String?, val choices: List<AiChoice>?, val usage: AiUsage)
internal data class AiDeltaEnvelope(val id: String?, val created: Int, val model: String?, val choices: List<AiDeltaChoice>?)
