package moe.via.browser.feature.search.suggestions

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import moe.via.browser.core.logging.NativeLog as Log
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.search.data.SearchBuiltins
import moe.via.browser.feature.search.data.SearchPreferences
import moe.via.browser.feature.search.model.SearchSuggestion
import moe.via.browser.feature.search.model.SearchSuggestionType
import moe.via.browser.feature.search.model.SearchTab
import moe.via.browser.feature.search.resolve.SearchCalculator
import moe.via.browser.feature.search.resolve.SearchUrlResolver
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** New input owns its local and online batches. Construction performs no query or network request. */
class SearchSuggestionService(context: Context) : Closeable {
    private val main = Handler(Looper.getMainLooper())
    private val preferences = SearchPreferences(context.applicationContext)
    private val local = SearchLocalSuggestions(context.applicationContext)
    private val generation = AtomicLong()
    private val closed = AtomicBoolean()
    private val cache = LruCache<String, List<SearchSuggestion>>(128)
    private var pending: Runnable? = null
    private var online: SearchSuggestionRequest? = null
    private var defaultProvider = 0
    private var previous: Query? = null
    private data class Query(val text: String, val tabs: List<SearchTab>, val mask: Int, val engine: Int, val defaultEngine: Int)

    fun query(text: String, tabs: List<SearchTab>, mask: Int = 31, engineId: Int, callback: (List<SearchSuggestion>) -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) { main.post { query(text, tabs, mask, engineId, callback) }; return }
        if (closed.get()) return
        val query = Query(text, tabs.toList(), mask, engineId, preferences.engineId)
        if (query == previous) return
        cancelCurrent()
        previous = query
        val request = generation.get()
        val execute = Runnable { if (active(request)) execute(query, request, callback) }
        pending = execute
        main.postDelayed(execute, 200L)
    }

    private fun execute(query: Query, request: Long, callback: (List<SearchSuggestion>) -> Unit) {
        pending = null
        if (query.text.length >= 10000) { callback(emptyList()); return }
        SearchCalculator.evaluate(query.text)?.let { value ->
            callback(listOf(SearchSuggestion(SearchSuggestionType.CALCULATOR, title = value, value = value)))
            return
        }
        val text = query.text.trim()
        var localItems = emptyList<SearchSuggestion>()
        var onlineItems = emptyList<SearchSuggestion>()
        BrowserExecutors.persistence.execute {
            val result = runCatching { local.query(text, query.tabs, query.mask) }
            main.post {
                if (!active(request)) return@post
                localItems = result.getOrDefault(emptyList())
                if (result.isFailure) Log.warn("Search", "Local suggestions unavailable: ${result.exceptionOrNull()?.javaClass?.simpleName}")
                callback(localItems + onlineItems)
            }
        }
        if (query.mask and 16 == 0 || text.isEmpty() || query.text.length > 200 || SearchUrlResolver.isHttp(query.text)) return
        val selectedProvider = SearchBuiltins.suggestionProvider(query.defaultEngine, 1)
        if (defaultProvider != selectedProvider) { defaultProvider = selectedProvider; cache.evictAll() }
        val provider = SearchBuiltins.suggestionProvider(query.engine, defaultProvider)
        val key = "$provider:$text"
        cache.get(key)?.let { onlineItems = it; callback(localItems + onlineItems); return }
        val token = SearchSuggestionRequest()
        online = token
        BrowserExecutors.network.execute {
            val result = runCatching { SearchOnlineSuggestions.fetch(text, provider, token) }
            main.post {
                if (!active(request) || token.cancelled.get()) return@post
                result.getOrNull()?.let { items -> cache.put(key, items); onlineItems = items }
                callback(localItems + onlineItems)
            }
        }
    }

    fun cancel() {
        previous = null
        cancelCurrent()
    }
    private fun cancelCurrent() {
        generation.incrementAndGet()
        pending?.let(main::removeCallbacks)
        pending = null
        online?.cancel()
        online = null
    }
    private fun active(request: Long) = !closed.get() && generation.get() == request
    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        cancel()
        cache.evictAll()
        BrowserExecutors.persistence.execute { local.close() }
    }
}
