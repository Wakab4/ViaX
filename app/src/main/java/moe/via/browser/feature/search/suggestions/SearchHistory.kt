package moe.via.browser.feature.search.suggestions

import moe.via.browser.feature.search.model.SearchSuggestion
import moe.via.browser.feature.search.model.SearchSuggestionType

/** A process-only search list. It does not read or write browser history. */
object SearchHistory {
    private val words = mutableListOf<String>()
    @Synchronized fun record(query: String, privateMode: Boolean = false, enabled: Boolean = true) {
        if (privateMode || !enabled) return
        val word = query.trim()
        if (word.isEmpty()) return
        words.remove(word)
        words.add(0, word)
    }
    @Synchronized fun clear() = words.clear()
    @Synchronized fun remove(word: String) { if (word.isEmpty()) words.clear() else words.remove(word) }
    @Synchronized fun suggestions(limit: Int = 5): List<SearchSuggestion> = words.take(limit.coerceAtLeast(0)).map {
        SearchSuggestion(SearchSuggestionType.SEARCH_HISTORY, title = it, value = it)
    }
}
