package moe.pure.browser.feature.search

import moe.pure.browser.model.BrowserTab
import moe.pure.browser.feature.search.model.SearchEngine
import moe.pure.browser.feature.search.resolve.SearchTemplate

internal object SearchHistoryNavigation {
    fun reuse(tab: BrowserTab, engine: SearchEngine, query: String, desktop: Boolean): Boolean = tab.reusePage { url ->
        query.equals(SearchTemplate.extract(url, engine.template(desktop)), ignoreCase = true)
    }
}
