package moe.via.browser.feature.search

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.home.HomeDocument
import moe.via.browser.feature.home.data.HomePreferences
import moe.via.browser.feature.network.NetworkLogPages
import moe.via.browser.feature.search.data.SearchEngineRepository
import moe.via.browser.feature.search.data.SearchPreferences
import moe.via.browser.feature.search.model.SearchEngine
import moe.via.browser.feature.search.model.SearchSuggestion
import moe.via.browser.feature.search.model.SearchSuggestionType
import moe.via.browser.feature.search.model.SearchTab
import moe.via.browser.feature.search.resolve.SearchCalculator
import moe.via.browser.feature.search.resolve.SearchResults
import moe.via.browser.feature.search.resolve.SearchShortcuts
import moe.via.browser.feature.search.resolve.SearchTemplate
import moe.via.browser.feature.search.resolve.SearchUrlResolver
import moe.via.browser.feature.search.suggestions.SearchHistory
import moe.via.browser.feature.search.suggestions.SearchSuggestionService
import moe.via.browser.feature.search.ui.SearchEngineToolbar
import moe.via.browser.feature.search.ui.SearchOverlay
import moe.via.browser.feature.search.ui.settings.SearchSettingsEntry
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.ui.theme.window.ChromeLayoutPolicy
import moe.via.browser.ui.theme.window.ChromeMode
import moe.via.browser.ui.components.BrowserMessage
import moe.via.browser.ui.dialog.BrowserDialog
import moe.via.browser.ui.theme.ThemedActivity
import moe.via.browser.ui.theme.palette

/** Coordinates the editor, suggestions and search navigation; chrome has no search data logic. */
class SearchController(private val activity: ThemedActivity, private val actions: Actions) {
    interface Actions {
        fun current(): BrowserTab?
        fun tabs(): List<BrowserTab>
        fun privateMode(): Boolean
        fun desktop(): Boolean
        fun pageHost(): ViewGroup
        fun beforeOpen()
        fun navigate(url: String, newTab: Boolean)
        fun openMany(urls: List<String>)
        fun switchTab(id: String)
        fun toolbar(view: View?)
    }
    private data class Engines(val all: List<SearchEngine>, val selected: SearchEngine)
    private val repository = SearchEngineRepository(activity)
    private val preferences = SearchPreferences(activity)
    private val suggestions = SearchSuggestionService(activity)
    private val main = Handler(Looper.getMainLooper())
    private val messages = BrowserMessage(activity)
    private var engines: Engines? = null
    private var openRequest = 0L
    private var disposed = false
    private var tabSnapshot = emptyList<SearchTab>()
    private var focusHost: ViewGroup? = null
    private var focusPolicy = ViewGroup.FOCUS_AFTER_DESCENDANTS
    private var inputState: SearchOverlay.InputState? = null
    private val addressActions = SearchAddressActions(activity, { show(it) }, { text, newTab -> submit(text, newTab = newTab) }, actions::openMany)
    private val overlayDelegate: Lazy<SearchOverlay> = lazy {
        SearchOverlay(activity, { activity.findViewById<FrameLayout>(android.R.id.content) }, object : SearchOverlay.Callbacks {
            override fun onInputChanged(text: String) = query(text)
            override fun onSubmit(text: String, temporaryEngineId: Int?) { submit(text, temporaryEngineId) }
            override fun onSuggestion(item: SearchSuggestion, temporaryEngineId: Int?) {
                when (item.type) {
                    SearchSuggestionType.CALCULATOR -> overlay.setInputText(item.value)
                    SearchSuggestionType.TAB -> { close(false); item.tabId?.let(actions::switchTab) }
                    else -> { close(false); submit(item.value, temporaryEngineId) }
                }
            }
            override fun onChooseEngine(anchor: View) = chooseEngine(anchor)
            override fun onClearSearchHistory() { SearchHistory.clear(); suggestions.cancel(); overlay.setSuggestions(emptyList()); overlay.setInputText(""); query("") }
            override fun onEngineCleared() { suggestions.cancel() }
            override fun onClosed() { suggestions.cancel(); restoreFocus(); inputState = null }
        })
    }
    private val overlay by overlayDelegate
    private val toolbarDelegate: Lazy<SearchEngineToolbar> = lazy { SearchEngineToolbar(activity, object : SearchEngineToolbar.Callbacks {
        override fun onEngine(engine: SearchEngine): Boolean {
            val tab = actions.current() ?: return false
            val match = engines?.all?.let { SearchResults.find(tab.url, it) } ?: return false
            if (!SearchHistoryNavigation.reuse(tab, engine, match.query, actions.desktop())) {
                val target = SearchTemplate.apply(engine.template(actions.desktop()), match.query)
                if (!SearchUrlResolver.isAddress(target)) {
                    activity.dialogScope.show(BrowserDialog(activity, "无效链接", target)
                        .positive("复制") {
                            activity.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, target))
                            messages.show("已复制文本"); true
                        }.negative())
                    return false
                }
                actions.navigate(target, false)
            }
            return true
        }
        override fun onSettings() = SearchSettingsEntry.open(activity)
    }) }
    private val toolbar by toolbarDelegate

    val isShowing get() = overlayDelegate.isInitialized() && overlay.isShowing

    fun show(explicitInput: String? = null, temporaryEngineId: Int? = null) {
        if (disposed) return
        if (overlay.isShowing) { explicitInput?.let { overlay.setInputText(it) }; return }
        val tab = actions.current() ?: return
        val request = ++openRequest
        actions.beforeOpen()
        readEngines { loaded ->
            if (request != openRequest || actions.current()?.id != tab.id) return@readEngines
            val host = actions.pageHost()
            focusHost = host; focusPolicy = host.descendantFocusability
            host.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS; host.clearFocus()
            tabSnapshot = actions.tabs().map { SearchTab(it.id, it.title, it.url) }
            val selected = loaded.selected
            val visible = preferences.visible(loaded.all)
            val toolbarMatch = if (preferences.toolbarEnabled && visible.size > 1) SearchResults.find(tab.url, visible) else null
            val initialEngineId = temporaryEngineId ?: toolbarMatch?.engineId?.takeIf { explicitInput.isNullOrEmpty() }
            val display = if (internal(tab)) "" else toolbarMatch?.query
                ?: SearchTemplate.extract(tab.url, selected.template(actions.desktop())) ?: tab.url
            val top = editorTop()
            val image = tab.home && !HomePreferences(activity).backgroundPath.isNullOrEmpty()
            val state = SearchOverlay.InputState(tab.url, display, explicitInput, actions.privateMode(),
                top, rounded = top && image, backgroundColor = editorColor(tab),
                temporaryEngine = loaded.all.firstOrNull { it.id == initialEngineId }, engines = loaded.all)
            inputState = state
            overlay.show(state)
        }
    }

    private fun query(text: String) {
        if (disposed || !overlay.isShowing) return
        val calculated = SearchCalculator.evaluate(text)
        if (calculated != null) {
            suggestions.cancel()
            overlay.setSuggestions(listOf(SearchSuggestion(SearchSuggestionType.CALCULATOR, calculated, value = calculated)))
            return
        }
        if (overlay.temporaryEngineId == null) {
            val match = SearchShortcuts.match(text, preferences.shortcuts())
            val engine = match?.let { quick -> engines?.all?.firstOrNull { it.id == quick.engineId } }
            if (match != null && engine != null) {
                overlay.setEngineBadge(engine); overlay.setInputText(match.query); return
            }
        }
        val id = overlay.temporaryEngineId ?: preferences.engineId
        suggestions.query(text, tabSnapshot, preferences.suggestionMask, id) { items ->
            if (!disposed && overlay.isShowing && overlay.text == text) overlay.setSuggestions(items)
        }
    }

    fun submit(text: String, temporaryEngineId: Int? = null, forceSearch: Boolean = false, newTab: Boolean = false, reuseExisting: Boolean = false) {
        if (disposed) return
        close(false)
        if (text.startsWith(SWITCH_TAB)) { actions.switchTab(text.removePrefix(SWITCH_TAB)); return }
        val perform: (Engines) -> Unit = { loaded ->
            val engine = loaded.all.firstOrNull { it.id == temporaryEngineId } ?: loaded.selected
            val result = SearchUrlResolver.resolve(text, engine.template(actions.desktop()), forceSearch || temporaryEngineId != null)
            if (result != null) {
                if (result.isSearch) SearchHistory.record(result.query.orEmpty(), actions.privateMode(), preferences.suggestionMask and 32 != 0)
                val existing = if (reuseExisting && SearchUrlResolver.isHttp(result.url)) actions.tabs().firstOrNull { it.url == result.url } else null
                if (existing != null) actions.switchTab(existing.id) else actions.navigate(result.url, newTab)
            }
        }
        val loaded = engines
        if (loaded != null && loaded.selected.id == preferences.engineId) perform(loaded) else readEngines(perform)
    }

    fun chooseEngine(anchor: View) {
        if (disposed) return
        readEngines { loaded ->
            activity.dialogScope.show(BrowserDialog(activity, "搜索引擎")
                .singleChoice(loaded.all.map(SearchEngine::name), loaded.all.indexOfFirst { it.id == preferences.engineId }) { index ->
                    val engine = loaded.all[index]
                    BrowserExecutors.persistence.execute {
                        val selected = runCatching { repository.select(engine.id) }.getOrDefault(false)
                        main.post {
                            if (!disposed && selected) {
                                engines = Engines(loaded.all, engine)
                                if (isShowing) { overlay.markGlobalEngineChanged(); query(overlay.text) }
                                refreshToolbar()
                            }
                        }
                    }
                })
        }
    }

    fun addressMenu(anchor: View) {
        val tab = actions.current() ?: return
        addressActions.show(anchor, tab.url, internal(tab))
    }

    fun consume(url: String): Boolean {
        if (!isSearchUrl(url)) return false
        val query = runCatching { Uri.parse(url).getQueryParameter("q") }.getOrNull()
        if (query.isNullOrEmpty()) show("") else submit(query, forceSearch = true)
        return true
    }

    fun consume(intent: Intent): Boolean {
        val action = intent.action
        val query = intent.getStringExtra(SearchManager.QUERY)
        val process = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
        val shared = if (action == Intent.ACTION_SEND) intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString() else null
        val search = action == SEARCH_ACTION || action == Intent.ACTION_SEARCH || action == Intent.ACTION_WEB_SEARCH
        val internal = intent.dataString?.takeIf(::isSearchUrl)
        if (query == null && process == null && shared == null && !search && internal == null) return false
        intent.action = Intent.ACTION_MAIN
        intent.removeExtra(SearchManager.QUERY); intent.removeExtra(Intent.EXTRA_PROCESS_TEXT); intent.removeExtra(Intent.EXTRA_TEXT)
        if (internal != null) { intent.data = null; consume(internal) }
        else if (query != null || process != null) submit(query ?: process.orEmpty(), forceSearch = true, newTab = true, reuseExisting = true)
        else if (shared != null) submit(shared, newTab = true, reuseExisting = true)
        else show("")
        return true
    }

    fun refresh() {
        if (!disposed) readEngines { loaded ->
            if (isShowing) {
                inputState = inputState?.copy(engines = loaded.all)
                if (overlay.temporaryEngineId?.let { id -> loaded.all.none { it.id == id } } == true) overlay.setEngineBadge(null)
                rebind()
                suggestions.cancel(); query(overlay.text)
            } else refreshToolbar()
        }
    }
    fun refreshToolbar() {
        if (disposed) return
        if (!preferences.toolbarEnabled) { actions.toolbar(null); return }
        val available = engines?.all ?: return
        val tab = actions.current()
        val match = tab?.let { SearchResults.find(it.url, available) }
        val visible = preferences.visible(available)
        if (match == null || visible.size <= 1) { actions.toolbar(null); return }
        toolbar.render(visible, match.engineId)
        actions.toolbar(toolbar.view.takeIf { it.visibility == View.VISIBLE })
    }

    fun rebind() {
        if (isShowing) {
            restoreFocus()
            focusHost = actions.pageHost().also { focusPolicy = it.descendantFocusability; it.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS }
            val tab = actions.current()
            inputState = inputState?.copy(top = editorTop(),
                rounded = editorTop() && tab?.home == true && !HomePreferences(activity).backgroundPath.isNullOrEmpty(),
                backgroundColor = tab?.let(::editorColor))
            overlay.rebind(inputState)
        }
        refreshToolbar()
    }
    fun back(): Boolean = overlayDelegate.isInitialized() && overlay.back()
    fun close(saveDraft: Boolean = true) { openRequest++; if (overlayDelegate.isInitialized()) overlay.close(saveDraft); restoreFocus() }
    private fun restoreFocus() { focusHost?.descendantFocusability = focusPolicy; focusHost = null }
    private fun editorTop() = ChromeLayoutPolicy.resolve(activity).mode.let { it == ChromeMode.TRADITIONAL || it == ChromeMode.TOP }
    private fun editorColor(tab: BrowserTab): Int? {
        val home = HomePreferences(activity)
        if (activity.palette.night || tab.home && !home.backgroundPath.isNullOrEmpty()) return null
        return if (tab.home) home.accentColor else activity.palette.canvas
    }
    private fun internal(tab: BrowserTab) = tab.home || HomeDocument.isHome(activity, tab.url) || tab.url == "about:blank" || NetworkLogPages.isPage(activity, tab.url)

    private fun readEngines(completed: (Engines) -> Unit) {
        BrowserExecutors.persistence.execute {
            val result = runCatching { Engines(repository.all(), repository.selected()) }
            main.post {
                if (!disposed) result.onSuccess { engines = it; completed(it) }
                    .onFailure { messages.show("无法读取搜索引擎") }
            }
        }
    }

    fun dispose() {
        disposed = true; openRequest++
        if (overlayDelegate.isInitialized()) overlay.dispose()
        restoreFocus(); suggestions.close()
        if (toolbarDelegate.isInitialized()) toolbar.dispose()
        addressActions.dispose(); messages.close()
        BrowserExecutors.persistence.execute { repository.close() }
    }

    companion object {
        const val SEARCH_ACTION = "moe.via.browser.SEARCH"
        private const val SWITCH_TAB = "VIA-SWITCH-TAB:"
        fun isSearchUrl(url: String): Boolean = runCatching {
            val uri = Uri.parse(url)
            uri.scheme in listOf("v", "via") && uri.host == "search"
        }.getOrDefault(false)
        fun accepts(intent: Intent) = intent.action in listOf(SEARCH_ACTION, Intent.ACTION_SEARCH, Intent.ACTION_WEB_SEARCH, Intent.ACTION_PROCESS_TEXT, Intent.ACTION_SEND) ||
            intent.hasExtra(SearchManager.QUERY) || intent.dataString?.let(::isSearchUrl) == true
    }
}
