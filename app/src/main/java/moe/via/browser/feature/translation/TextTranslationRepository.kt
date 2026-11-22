package moe.via.browser.feature.translation

import android.os.Handler
import android.os.Looper
import moe.via.browser.core.logging.NativeLog as Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Own one instance per panel. The reference cache deliberately omits source from its key. */
class TextTranslationRepository : AutoCloseable {
    private val main = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "text-translation").apply { isDaemon = true }
    }
    private val gate = Any()
    private val cache = linkedMapOf<Pair<String, String>, TranslationResult>()
    private var active: Request? = null
    private var generation = 0L
    private var closed = false

    private class Request(val generation: Long) {
        val cancelled = AtomicBoolean()
        val connection = AtomicReference<HttpURLConnection?>()
        @Volatile var future: Future<*>? = null
        fun cancel() {
            cancelled.set(true)
            future?.cancel(true)
            runCatching { connection.getAndSet(null)?.disconnect() }
        }
    }

    fun translate(query: String, source: String?, target: String, callback: (TranslationResult?) -> Unit) {
        val request: Request
        val previous: Request?
        val cached: TranslationResult?
        val text = query.trim()
        synchronized(gate) {
            if (closed) return
            previous = active
            request = Request(++generation)
            active = request
            cached = cache[text to target]
        }
        previous?.cancel()
        if (text.isEmpty() || target.isEmpty() || cached != null) {
            deliver(request, cached, callback)
            return
        }
        val requestedSource = source?.takeIf(String::isNotEmpty) ?: "auto"
        try {
            request.future = executor.submit {
                val result = try {
                    fetch(request, text, requestedSource, target)
                } catch (error: Exception) {
                    if (!request.cancelled.get()) Log.warn("Translation", "Text translation failed", error)
                    null
                }
                synchronized(gate) {
                    if (!closed && active === request && !request.cancelled.get() && result != null) cache[text to target] = result
                }
                deliver(request, result, callback)
            }
            if (request.cancelled.get()) request.future?.cancel(true)
        } catch (error: RuntimeException) {
            if (!request.cancelled.get()) Log.warn("Translation", "Cannot schedule text translation", error)
            deliver(request, null, callback)
        }
    }

    private fun fetch(request: Request, query: String, source: String, target: String): TranslationResult? {
        if (request.cancelled.get()) return null
        val url = "https://translate.google.com/translate_a/single?dj=1&sl=${TranslationUrls.encode(source)}&tl=${TranslationUrls.encode(target)}&ie=UTF-8&oe=UTF-8&client=at&dt=t&otf=2&q=${TranslationUrls.encode(query)}"
        val connection = URL(url).openConnection() as HttpURLConnection
        request.connection.set(connection)
        try {
            if (request.cancelled.get()) return null
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", USER_AGENT)
            // Bound a stalled request while retaining the original request and response shape.
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            if (connection.responseCode != HttpURLConnection.HTTP_OK || request.cancelled.get()) return null
            val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                buildString {
                    while (!request.cancelled.get()) {
                        val line = reader.readLine() ?: break
                        append(line.trim())
                    }
                }
            }
            if (request.cancelled.get()) return null
            val translated = JSONObject(response).optJSONArray("sentences")?.optJSONObject(0)?.optString("trans").orEmpty()
            return translated.takeIf(String::isNotEmpty)?.let { TranslationResult(query, it, source, target) }
        } finally {
            request.connection.compareAndSet(connection, null)
            connection.disconnect()
        }
    }

    private fun deliver(request: Request, result: TranslationResult?, callback: (TranslationResult?) -> Unit) {
        main.post {
            val current = synchronized(gate) {
                if (closed || request.cancelled.get() || active !== request || generation != request.generation) false
                else { active = null; true }
            }
            if (current) callback(result)
        }
    }

    fun cancel() {
        val previous = synchronized(gate) { generation++; active.also { active = null } }
        previous?.cancel()
    }

    override fun close() {
        synchronized(gate) { closed = true; cache.clear() }
        cancel()
        executor.shutdownNow()
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36"
    }
}
