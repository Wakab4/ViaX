package moe.pure.browser.feature.home.search

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.search.data.SearchBuiltins
import moe.pure.browser.feature.search.data.SearchPreferences
import moe.pure.browser.feature.search.resolve.SearchUrlResolver
import moe.pure.browser.feature.search.suggestions.SearchHistory
import moe.pure.browser.feature.search.suggestions.SearchOnlineSuggestions
import moe.pure.browser.feature.search.suggestions.SearchSuggestionRequest

/** ua.C1 uses online suggestions for text and process search history for an empty field. */
internal class HomeSearchSuggestions(context: Context) : AutoCloseable {
    private val preferences = SearchPreferences(context.applicationContext)
    private val main = Handler(Looper.getMainLooper())
    private val cache = LruCache<String, List<String>>(128)
    private var request: SearchSuggestionRequest? = null
    private var selectedEngine: Int? = null
    private var closed = false

    fun query(text: String, callback: (List<String>) -> Unit) {
        cancel()
        val mask = preferences.suggestionMask
        if (closed || mask == 0) return
        val engine = preferences.engineId
        if (text.isNotEmpty() && selectedEngine != engine) {
            selectedEngine = engine
            synchronized(cache) { cache.evictAll() }
        }
        val pending = SearchSuggestionRequest()
        request = pending
        BrowserExecutors.network.execute {
            val result = runCatching {
                when {
                    text.isEmpty() -> if (mask and 32 != 0) SearchHistory.suggestions().map { it.value } else emptyList()
                    mask and 16 == 0 || text.length > 200 || SearchUrlResolver.isHttp(text) -> emptyList()
                    else -> {
                        val query = text.trim { it <= ' ' }
                        if (query.isEmpty()) emptyList()
                        else synchronized(cache) { cache.get(query) } ?: run {
                            val provider = SearchBuiltins.suggestionProvider(engine, 1)
                            SearchOnlineSuggestions.fetch(query, provider, pending)?.map { it.value }?.also {
                                if (!pending.cancelled.get()) synchronized(cache) { cache.put(query, it) }
                            }.orEmpty()
                        }
                    }
                }
            }
            result.onSuccess { words ->
                main.post { if (!closed && request === pending && !pending.cancelled.get()) callback(words) }
            }.onFailure { moe.pure.browser.core.logging.NativeLog.warn("HomeSuggestions", "Request suggestions", it) }
        }
    }

    fun cancel() { request?.cancel(); request = null }
    override fun close() { closed = true; cancel(); synchronized(cache) { cache.evictAll() } }
}
