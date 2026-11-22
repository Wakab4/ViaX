package moe.via.browser.feature.ai.network

import moe.via.browser.feature.ai.network.model.AiWireMessage
/** Cancellation disconnects HTTP; an opened chat stream also delivers its native terminal close event. */
interface AiCall {
    val isCancelled: Boolean
    fun cancel()
}

data class AiCompletion(val message: AiWireMessage?, val finishReason: String? = null) {
    val content get() = message?.content
    val reasoning get() = message?.reasoning
}

/** Content and reasoning are cumulative snapshots, not append-only fragments. */
data class AiStreamResult(
    val status: Int,
    val message: AiWireMessage? = null,
    val finishReason: String? = null,
    val error: AiException? = null
) {
    val terminal: Boolean get() = status != 0
    val isError: Boolean get() = status != 0 && status != 1
    val content: String get() = message?.content.orEmpty()
    val reasoning: String? get() = message?.reasoning
}
