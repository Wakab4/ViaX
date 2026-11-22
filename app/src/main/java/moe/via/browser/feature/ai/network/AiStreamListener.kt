package moe.via.browser.feature.ai.network

import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.ai.network.model.AiToolCall
import moe.via.browser.feature.ai.network.model.AiToolFunction
import moe.via.browser.feature.ai.network.model.AiWireMessage
import moe.via.browser.feature.ai.network.sse.NativeAiEventSource

/** p9.m.a accumulates first-choice deltas, and keeps its mutable message across EOF reconnects. */
internal class AiStreamListener(private val active: () -> Boolean, private val close: () -> Unit,
    private val callback: (AiStreamResult) -> Unit) : NativeAiEventSource.Handler {
    private var message: AiWireMessage? = null
    private var finished = false
    private var finishReason: String? = null
    private var lastUpdate = 0L
    override fun event(type: String, value: String) {
        if (value == "[DONE]") { finished = true; callback(AiStreamResult(1, message, finishReason)); close(); return }
        if (value.isEmpty()) return
        try {
            val choices = AiWireCodec.delta(value).choices!!
            if (choices.isEmpty()) return
            val first = choices[0]
            if (!first.finishReason.isNullOrEmpty()) finishReason = first.finishReason
            val delta = first.message
            val target = message ?: AiWireMessage(role = delta.role).also { message = it }
            delta.content?.let { target.content = target.content.orEmpty() + it }
            delta.reasoning?.let { target.reasoning = target.reasoning.orEmpty() + it }
            delta.toolCalls?.let { tools ->
                val list = target.toolCalls ?: ArrayList<AiToolCall>().also { target.toolCalls = it }
                tools.forEach { tool ->
                    if (list.size <= tool.index) list.add(AiToolCall("", AiToolFunction("", ""), "function", tool.index))
                    val current = list[tool.index]
                    tool.id?.let { current.id = current.id + it }
                    tool.function?.let { function ->
                        val existing = current.function!!
                        function.name?.let { existing.name = existing.name + it }
                        function.arguments?.let { existing.arguments = existing.arguments + it }
                    }
                }
            }
            val now = System.currentTimeMillis()
            if (now - lastUpdate > 50L) { lastUpdate = now; callback(AiStreamResult(0, target)) }
        } catch (error: Exception) {
            NativeLog.warn(null, error)
            if (!active()) return
            finished = true; callback(AiStreamResult(100, message, finishReason, mapped(error))); close()
        }
    }
    override fun closed() { if (!finished) callback(AiStreamResult(100, message, finishReason, AiException(100, finishReason))) }
    override fun error(error: Throwable) {
        if (!active()) return
        finished = true; NativeLog.warn(null, error)
        callback(AiStreamResult(100, message, finishReason, mapped(error))); close()
    }
    private fun mapped(error: Throwable) = if (error is AiException) error else AiException(100, finishReason)
}
