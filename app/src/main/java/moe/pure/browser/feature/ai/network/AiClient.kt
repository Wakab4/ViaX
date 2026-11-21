package moe.pure.browser.feature.ai.network

import android.os.Handler
import android.os.Looper
import java.util.concurrent.TimeUnit
import moe.pure.browser.feature.ai.model.AiBackend
import moe.pure.browser.feature.ai.network.model.AiWireMessage
import moe.pure.browser.feature.ai.network.sse.NativeAiEventSource
import okhttp3.OkHttpClient

/** p9.v/m share the active EventSource; ordinary completions use the same OkHttp client. */
class AiClient {
    private val main = Handler(Looper.getMainLooper())
    private val http = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
    private var activeStream: NativeAiEventSource? = null
    fun stopStream(): Boolean {
        val source = activeStream ?: return false
        val connecting = source.state == NativeAiEventSource.State.CONNECTING
        source.cancel(); activeStream = null; return connecting
    }
    fun stream(backend: AiBackend?, modelId: String?, messages: List<AiWireMessage>, callback: (AiStreamResult) -> Unit) {
        if (messages.isEmpty()) { callback(AiStreamResult(101, error = AiException(101))); return }
        if (backend == null) { callback(AiStreamResult(102, error = AiException(102))); return }
        if (AiRequestCodec.invalid(backend)) { callback(AiStreamResult(104, error = AiException(104))); return }
        val request = AiRequestCodec.request(backend, modelId, messages, true)
        activeStream?.cancel()
        val listener = AiStreamListener({ activeStream != null }, { activeStream!!.cancel(); activeStream = null }, callback)
        val source = NativeAiEventSource(http, request, listener)
        activeStream = source; source.start()
    }
    /** a9.r0.g consumes the first callback, then closes the shared stream even for a failure. */
    fun validate(backend: AiBackend, callback: (Result<Unit>) -> Unit) {
        var received = false
        stream(backend, backend.models.firstOrNull(), listOf(AiWireMessage("Say 'test'", "user"))) { value ->
            if (!received) {
                received = true; stopStream()
                val result = if (!value.isError) Result.success(Unit) else Result.failure(value.error ?: AiException(value.status, value.finishReason))
                main.post { callback(result) }
            }
        }
    }
    /** p9.v.B is synchronous IO; model-owned work decides its lifetime and main delivery. */
    fun complete(backend: AiBackend?, modelId: String?, messages: List<AiWireMessage>): Result<AiCompletion> {
        if (messages.isEmpty()) return Result.failure(AiException(101))
        if (backend == null) return Result.failure(AiException(102))
        return try {
            if (AiRequestCodec.invalid(backend)) throw IllegalArgumentException("Invalid backend")
            http.newCall(AiRequestCodec.request(backend, modelId, messages, false)).execute().use { response ->
                if (response.code != 200) throw AiException(100, httpCode = response.code, responseBody = response.body.string())
                val choices = AiWireCodec.completion(response.body.string()).choices!!
                if (choices.isEmpty()) Result.failure(AiException(103)) else Result.success(AiCompletion(choices[0].message, choices[0].finishReason))
            }
        } catch (error: Exception) { Result.failure(if (error is AiException) error else AiException(100)) }
    }
}
