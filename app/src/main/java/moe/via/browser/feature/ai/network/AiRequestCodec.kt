package moe.via.browser.feature.ai.network

import moe.via.browser.core.logging.NativeLog
import moe.via.browser.feature.ai.model.AiBackend
import moe.via.browser.feature.ai.network.model.AiWireMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/** p9.m.i/g accepts the original network/file prefixes; OkHttp performs actual URL construction. */
internal object AiRequestCodec {
    fun invalid(backend: AiBackend): Boolean {
        val url = backend.baseUrl
        return !(url.startsWith("http://", true) || url.startsWith("https://", true) || url.startsWith("file://", true)) ||
            backend.token.isEmpty() || backend.token.any { (it.code <= 31 && it != '\t') || it.code >= 127 }
    }
    fun request(backend: AiBackend, model: String?, messages: List<AiWireMessage>, streaming: Boolean): Request {
        val url = backend.baseUrl.let { if (it.endsWith("/chat/completions")) it else it + if (it.endsWith('/')) "chat/completions" else "/chat/completions" }
        val text = try { AiWireCodec.request(model, messages, streaming) } catch (error: Exception) { NativeLog.warn(null, error); "" }
        return Request.Builder().url(url).header("Authorization", "Bearer ${backend.token}")
            .header("User-Agent", "okhttp/3.12.0")
            .post(text.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
    }
}
