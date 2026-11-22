package moe.via.browser.feature.ai.network

import moe.via.browser.feature.ai.network.model.*
import org.json.JSONArray
import org.json.JSONObject

/** p9.a/b/c/d/e/f/i/j/k/s: parse every choice, preserve nullable fields and use the first later. */
internal object AiWireCodec {
    private fun string(json: JSONObject, key: String): String? = if (json.isNull(key)) null else json.getString(key)
    private fun <T> array(values: JSONArray?, row: (JSONObject) -> T): List<T>? = values?.let { source -> List(source.length()) { row(source.getJSONObject(it)) } }
    fun message(json: JSONObject): AiWireMessage {
        val result = AiWireMessage()
        result.content = string(json, "content"); result.reasoning = string(json, "reasoning_content")
        result.toolCalls = array(json.optJSONArray("tool_calls")) { tool ->
            AiToolCall(string(tool, "id"), tool.optJSONObject("function")?.let { AiToolFunction(string(it, "name"), string(it, "arguments")) },
                string(tool, "type"), tool.optInt("index", 0))
        }?.takeIf { it.isNotEmpty() }?.toMutableList()
        result.toolCallId = string(json, "tool_call_id"); result.role = string(json, "role")
        return result
    }
    fun completion(text: String): AiCompletionEnvelope {
        val json = JSONObject(text)
        val id = string(json, "id"); val created = json.getInt("created"); val model = string(json, "model")
        val usage = json.getJSONObject("usage")
        val tokens = AiUsage(usage.optInt("prompt_tokens", 0), usage.optInt("completion_tokens", 0), usage.optInt("total_tokens", 0))
        val choices = array(json.optJSONArray("choices")) { choice -> AiChoice(choice.optInt("index", 0), choice.optJSONObject("message")?.let(::message), string(choice, "finish_reason")) }
        return AiCompletionEnvelope(id, created, model, choices, tokens)
    }
    fun delta(text: String): AiDeltaEnvelope {
        val json = JSONObject(text)
        return AiDeltaEnvelope(string(json, "id"), json.optInt("created", 0), string(json, "model"),
            array(json.optJSONArray("choices")) { choice -> AiDeltaChoice(choice.optInt("index", 0), message(choice.getJSONObject("delta")), string(choice, "finish_reason")) })
    }
    fun request(model: String?, messages: List<AiWireMessage>, stream: Boolean): String = JSONObject().apply {
        put("model", model); put("stream", stream); put("temperature", .5)
        put("messages", JSONArray().apply { messages.forEach { put(encode(it)) } })
    }.toString()
    private fun encode(value: AiWireMessage) = JSONObject().apply {
        put("content", value.content); put("role", value.role)
        value.toolCalls?.takeIf { it.isNotEmpty() }?.let { tools -> put("tool_calls", JSONArray().apply {
            tools.forEach { tool -> put(JSONObject().apply {
                put("id", tool.id)
                tool.function?.let { put("function", JSONObject().put("name", it.name).put("arguments", it.arguments)) }
                put("type", tool.type)
            }) }
        }) }
        value.toolCallId?.let { put("tool_call_id", it) }
    }
}
