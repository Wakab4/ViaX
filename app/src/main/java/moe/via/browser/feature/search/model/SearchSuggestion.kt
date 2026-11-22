package moe.via.browser.feature.search.model

object SearchSuggestionType {
    const val SEARCH = 0
    const val FAVORITE = 1
    const val BOOKMARK = 2
    const val TAB = 4
    const val HISTORY = 8
    const val ONLINE = 16
    const val SEARCH_HISTORY = 32
    const val CALCULATOR = 64
}

data class SearchSuggestion(
    val type: Int,
    val title: String = "",
    val url: String = "",
    val value: String = url.ifEmpty { title },
    val tabId: String? = null
) {
    val id: String get() = "$type:${tabId ?: url.ifEmpty { value }}"
}

data class SearchTab(val id: String, val title: String, val url: String)
data class SearchQuickMatch(val engineId: Int, val query: String)
data class ShortcutResult(val valid: Boolean, val error: String? = null)
data class ResolvedSearchInput(val url: String, val isSearch: Boolean, val query: String? = null)
