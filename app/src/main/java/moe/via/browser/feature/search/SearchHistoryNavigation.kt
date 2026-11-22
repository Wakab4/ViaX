package moe.via.browser.feature.search

import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.feature.search.model.SearchEngine
import moe.via.browser.feature.search.resolve.SearchTemplate

internal object SearchHistoryNavigation {
    fun reuse(tab: BrowserTab, engine: SearchEngine, query: String, desktop: Boolean): Boolean = tab.reusePage { url ->
        query.equals(SearchTemplate.extract(url, engine.template(desktop)), ignoreCase = true)
    }
}
