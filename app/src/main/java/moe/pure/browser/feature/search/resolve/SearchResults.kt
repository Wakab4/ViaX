package moe.pure.browser.feature.search.resolve

import moe.pure.browser.feature.search.model.SearchEngine

data class SearchResultMatch(val engineId: Int, val query: String)

object SearchResults {
    fun find(url: String, engines: List<SearchEngine>): SearchResultMatch? {
        engines.forEach { engine ->
            listOf(engine.mobileTemplate, engine.desktopTemplate).distinct().forEach { template ->
                SearchTemplate.extract(url, template)?.let { return SearchResultMatch(engine.id, it) }
            }
        }
        return null
    }
    fun historyIndex(urls: List<String>, engine: SearchEngine, query: String): Int? = urls.indices.firstOrNull { index ->
        listOf(engine.mobileTemplate, engine.desktopTemplate).any { SearchTemplate.extract(urls[index], it) == query }
    }
}
