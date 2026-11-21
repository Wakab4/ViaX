package moe.pure.browser

import moe.pure.browser.ui.components.insets.NativeFullscreenState
import moe.pure.browser.ui.components.insets.NativeSystemBarAppearance
import moe.pure.browser.feature.settings.skin.NativeSkinIcons
import moe.pure.browser.core.app.NativeAppRestart
import moe.pure.browser.browser.startup.NativeStartupVersion

import moe.pure.browser.ui.theme.ThemedActivity
import moe.pure.browser.ui.theme.palette
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import moe.pure.browser.web.bridge.NativePageMessage
import moe.pure.browser.browser.BrowserSession
import moe.pure.browser.browser.BrowserInteractionController
import moe.pure.browser.feature.gestures.BrowserInputState
import moe.pure.browser.feature.settings.data.ActionBinding
import moe.pure.browser.feature.settings.SettingsEntry
import moe.pure.browser.feature.settings.ui.ClearDataController
import moe.pure.browser.feature.appearance.PageAppearanceController
import moe.pure.browser.feature.contextmenu.BrowserPageContextMenu
import moe.pure.browser.feature.contextmenu.ContextMenuPageState
import moe.pure.browser.feature.contextmenu.PageImageModeController
import moe.pure.browser.feature.customtabs.CustomTabController
import moe.pure.browser.web.error.BrowserErrorController
import moe.pure.browser.feature.entry.BrowserEntry
import moe.pure.browser.feature.entry.BrowserEntryController
import moe.pure.browser.feature.firstrun.FirstRunController
import moe.pure.browser.feature.engine.EngineAvailabilityController
import moe.pure.browser.feature.library.dialog.BookmarksDialogController
import moe.pure.browser.browser.closed.ClosedTabsRestorer
import moe.pure.browser.browser.UndoCloseController
import moe.pure.browser.feature.home.HomeDocument
import moe.pure.browser.feature.home.HomeController
import moe.pure.browser.feature.history.HistoryDirtyState
import moe.pure.browser.feature.history.document.HistoryDocumentController
import moe.pure.browser.feature.history.document.HistoryDocumentOptions
import moe.pure.browser.feature.internalpages.GeneratedAddress
import moe.pure.browser.feature.internalpages.GeneratedPageController
import moe.pure.browser.feature.internalpages.GeneratedPageDirtyState
import moe.pure.browser.feature.bookmarks.document.BookmarkDocumentActions
import moe.pure.browser.feature.library.popup.BookmarkPopupController
import moe.pure.browser.feature.offline.document.SavedDocumentActions
import moe.pure.browser.feature.startpage.ConfiguredStartPageController
import moe.pure.browser.feature.startpage.StartupRestoreNotice
import moe.pure.browser.feature.startpage.document.ConfiguredStartPageDocuments
import moe.pure.browser.feature.home.ui.favorites.HomeFavoritesController
import moe.pure.browser.feature.pageinfo.PageInfoController
import moe.pure.browser.feature.scan.QrCodeDialog
import moe.pure.browser.feature.network.NetworkToolsController
import moe.pure.browser.feature.network.NetworkContextMenu
import moe.pure.browser.feature.adblock.CustomBlockRules
import moe.pure.browser.feature.site.SiteSettingsRepository
import moe.pure.browser.feature.site.SiteFeature
import moe.pure.browser.feature.site.ui.SiteSettingsNavigation
import moe.pure.browser.web.WebViewPolicyController
import moe.pure.browser.web.permissions.WebPermissionsController
import moe.pure.browser.data.BrowserStore
import moe.pure.browser.data.BrowserSettings
import moe.pure.browser.feature.menu.BrowserMenuController
import moe.pure.browser.feature.menu.MenuAction
import moe.pure.browser.feature.menu.MenuContext
import moe.pure.browser.feature.find.FindInPageController
import moe.pure.browser.feature.reader.ReaderController
import moe.pure.browser.feature.readaloud.ReadAloudController
import moe.pure.browser.feature.translation.TranslationController
import moe.pure.browser.feature.game.GameModeController
import moe.pure.browser.feature.search.SearchController
import moe.pure.browser.feature.search.data.remote.RemoteSearchConfig
import moe.pure.browser.feature.tools.PageToolDialogs
import moe.pure.browser.feature.useragent.UserAgentRepository
import moe.pure.browser.feature.useragent.UserAgentDialogs
import moe.pure.browser.feature.library.LibraryRepository
import moe.pure.browser.feature.library.pages.LibraryNavigationEntry
import moe.pure.browser.feature.library.LibraryTab
import moe.pure.browser.feature.library.navigation.LibraryOpenResult
import moe.pure.browser.feature.library.navigation.LibraryOpenWhere
import moe.pure.browser.feature.library.SavePageController
import moe.pure.browser.feature.download.DownloadController
import moe.pure.browser.model.BrowserTab
import moe.pure.browser.ui.BrowserChrome
import moe.pure.browser.ui.tabs.TabSwitcher
import moe.pure.browser.ui.tabs.BrowserTabStrip
import moe.pure.browser.web.BrowserExternalActions
import moe.pure.browser.web.BrowserWebViewFactory
import moe.pure.browser.web.navigation.redirect.PageRedirectController
import java.util.concurrent.Executors
import moe.pure.browser.browser.host.BrowserRootHost
import moe.pure.browser.ui.pages.PredictiveBackPreview
import moe.pure.browser.ui.pages.WebBackPreview

open class BrowserActivity : ThemedActivity(), moe.pure.browser.ui.chrome.BrowserWindowMode {
    override val customTabMode: Boolean get() = false
    private val gameActive get() = !customTabMode && GameModeController.active
    private val privateMode get() = settings.preferences.incognito
    private var pendingSearchIntent: Intent? = null
    private var searchEntryConsumed = false
    private var restoringSession = false
    private var queuedEntryIntent: Intent? = null
    private var browserStarted = false
    private var browserGeneration = 0L
    private var activityResumed = false
    private var initialWindowLayoutUpdated = false
    private var pendingBrowserState: Bundle? = null
    private var pendingFeatureState: Bundle? = null
    private var featuresRestored = false
    private var lastSystemBackAt = 0L
    private var lastPageMarkerAt = 0L
    private val rootHost: BrowserRootHost by lazy { BrowserRootHost(this, ::onBrowserRootVisibilityChanged, ::onSiteSettingsResult, ::onLibraryResult, ::onScannerResult) }
    private val firstRunDelegate = lazy { FirstRunController(this, preferences, object : FirstRunController.Host {
        override fun showContent(view: View) = rootHost.showGate(view)
        override fun beginBrowser() = startBrowser(pendingBrowserState)
        override fun exit() = finish()
    }) }
    private val firstRun by firstRunDelegate
    private val engineDelegate = lazy { EngineAvailabilityController(this, object : EngineAvailabilityController.Host {
        override fun showContent(view: View) = rootHost.showGate(view)
        override fun retryBrowser() {
            NativeAppRestart.restart(this@BrowserActivity)
        }
        override fun exit() { finish(); kotlin.system.exitProcess(0) }
    }) }
    private val engine by engineDelegate
    private val remoteSearchDelegate = lazy { RemoteSearchConfig(applicationContext) }
    private val remoteSearch by remoteSearchDelegate
    private val entriesDelegate = lazy { BrowserEntryController(this) }
    private val entries by entriesDelegate

    private val sessionDelegate: Lazy<BrowserSession> = lazy { BrowserSession(this, { privateMode }, ::configureWebView, object : BrowserSession.Callbacks {
        override fun changed() {
            if (!browserStarted) return
            refreshNavigation()
            if (::switcher.isInitialized) switcher.refresh()
        }
        override fun closed(batch: moe.pure.browser.browser.ClosedTabBatch) {
            if (settings.preferences.undoClosePrompt) undoClose.show(batch) { session.undo(batch) }
        }
        override fun persistenceError(error: Throwable) {
            moe.pure.browser.core.logging.NativeLog.error("BrowserSession", "Cannot persist browser session", error)
            toast("标签状态保存失败：${error.message}")
        }
        override fun engineUnavailable(error: moe.pure.browser.web.compat.WebEngineStartupException) {
            browserStarted = false
            browserGeneration++
            restoringSession = false
            if (entriesDelegate.isInitialized()) entries.cancelPending()
            if (::switcher.isInitialized) switcher.close(animate = false)
            moe.pure.browser.core.logging.NativeLog.error("BrowserEngine", "Cannot construct WebView", error)
            engine.showInitializationFailure()
        }
        override fun requestDefaultPage() {
            if (browserReady()) {
                if (customTabMode) finish() else requestConfiguredTab()
            }
        }
    }, pageNavigation, object : BrowserTab.PageHost {
        override fun prepare(tab: BrowserTab, view: WebView, url: String?) {
            applyWebSettings(tab, view, url ?: tab.sourceUrl(view) ?: tab.url)
        }
        override fun changed(tab: BrowserTab, previous: WebView?, current: WebView?, flags: Int) {
            if (!browserReady()) return
            if (current != null) tab.desktop = siteRepository.resolve(tab.url, SiteFeature.DESKTOP, settings) == moe.pure.browser.feature.site.SiteDecision.ALLOW
            if (flags and 1 == 0 || tab !== currentTab() || current == null) return
            if (flags and 8 != 0 && current.parent === chrome.pageFocusHost) {
                appearance.selected(tab)
                refreshChrome()
                publishSelectedAddress(tab)
            } else renderTab(tab)
            if (::switcher.isInitialized) switcher.refresh()
        }
    }, webPolicy::isPrivate, persistentSession = !customTabMode) }
    private val session: BrowserSession by sessionDelegate
    private val tabs: List<BrowserTab> get() = session.tabs
    private val selected: Int get() = session.selectedIndex
    private lateinit var chrome: BrowserChrome
    private lateinit var webFactory: BrowserWebViewFactory
    private lateinit var external: BrowserExternalActions
    private var policyFingerprint: String? = null
    private lateinit var switcher: TabSwitcher
    private var tabStrip: BrowserTabStrip? = null
    private var chromeForeground: Int? = null
    private val preferences: SharedPreferences by lazy { getSharedPreferences("browser", MODE_PRIVATE) }
    private val settings by lazy { BrowserSettings(this) }
    private val agentRepositoryDelegate = lazy { UserAgentRepository(this) }
    private val agentRepository by agentRepositoryDelegate
    private val siteRepositoryDelegate = lazy { SiteSettingsRepository(this) }
    private val siteRepository by siteRepositoryDelegate
    private val webPolicy by lazy { WebViewPolicyController(this, settings, siteRepository, agentRepository) }
    private val pageNavigation by lazy { moe.pure.browser.web.navigation.stack.PageStackNavigationPolicy(applicationContext,
        globalBackCache = { settings.preferences.backCache }, site = { pageSitePolicy(siteRepository.forUrl(it)) },
        popupSite = { pageSitePolicy(siteRepository.get(moe.pure.browser.feature.site.NativeSiteKeys.popupDestination(it))) }) }

    private fun pageSitePolicy(config: moe.pure.browser.feature.site.SiteConfiguration?) = config?.let {
        moe.pure.browser.web.navigation.stack.PageStackSitePolicy(enabled = it.enabled,
            backCache = it.values[SiteFeature.BACK_CACHE]?.let { value -> value == moe.pure.browser.feature.site.SiteDecision.ALLOW },
            userAgentChoice = it.values[SiteFeature.USER_AGENT] ?: -1000, customUserAgent = it.customUserAgent,
            desktopOverride = it.values[SiteFeature.DESKTOP]?.let { value -> value == moe.pure.browser.feature.site.SiteDecision.ALLOW },
            hasSettings = it.native?.let { raw -> !raw.isEmpty() } ?: (!it.customUserAgent.isNullOrEmpty() || it.values.isNotEmpty()))
    }
    private val redirectsDelegate = lazy { PageRedirectController(this, settings, siteRepository, object : PageRedirectController.Host {
        override fun currentTab() = if (sessionDelegate.isInitialized()) this@BrowserActivity.currentTab() else null
        override fun pageVisible() = browserReady() && chrome.root.windowToken != null && chrome.root.visibility == View.VISIBLE
        override fun elementPickerActive() = pickerDelegate.isInitialized() && picker.isActive
        override fun navigate(tab: BrowserTab, url: String) { if (browserReady()) load(tab, url) }
    }) }
    private val redirects by redirectsDelegate
    private val webPermissionsDelegate = lazy { WebPermissionsController(this, settings, siteRepository,
        canPresentExternalApp = { browserReady() && supportFragmentManager.backStackEntryCount == 0 },
        currentExternalOrigin = { currentTab()?.liveView?.url ?: currentTab()?.url }) }
    private val webPermissions by webPermissionsDelegate
    private val customTabsDelegate = lazy { CustomTabController(this, object : CustomTabController.Host {
        override fun currentUrl() = currentTab()?.url.orEmpty()
        override fun currentTitle() = currentTab()?.title.orEmpty()
        override fun share() { currentTab()?.let(this@BrowserActivity::shareCurrentPage) }
        override fun addBookmark() { currentTab()?.let(::toggleBookmark) }
        override fun find() { currentTab()?.let { this@BrowserActivity.find.show(it.webView) } }
        override fun translate() { currentTab()?.let(translation::translatePage) }
        override fun scriptsAvailable() = currentTab()?.let(scripts::pageHasScripts) == true
        override fun showScripts() { currentTab()?.let { scripts.show(it, false) } }
        override fun openInBrowser() {
            currentTab()?.let { tab -> startActivity(Intent(this@BrowserActivity, BrowserActivity::class.java)
                .setAction(Intent.ACTION_VIEW).setData(android.net.Uri.parse(tab.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
        override fun finish() = this@BrowserActivity.finish()
    }) }
    private val customTabs by customTabsDelegate
    private val errorsDelegate = lazy { BrowserErrorController(object : BrowserErrorController.Host {
        override fun routeUnknownScheme(tab: BrowserTab, view: WebView, url: String) = routePageUrl(tab, view, url)
        override fun navigate(tab: BrowserTab, view: WebView, url: String) {
            if (!routePageUrl(tab, view, url)) navigateSource(tab, view, url)
        }
    }) }
    private val errors by errorsDelegate
    private val toolsDelegate = lazy { PageToolDialogs(this, settings) }
    private val tools by toolsDelegate
    private val pageFontDelegate = lazy { moe.pure.browser.feature.tools.font.PageFontDialog(this, settings, siteRepository,
        { currentTab()?.liveView }, ::applyWebSettings, { flags ->
            currentTab()?.let { SiteSettingsNavigation.open(this, it.url, flags) }
        }) }
    private val pageFont by pageFontDelegate
    private val historyVisitsDelegate = lazy { moe.pure.browser.feature.history.HistoryVisitController(applicationContext,
        webPolicy::isPrivate, session::invalidateRecentClosed) }
    private val historyVisits by historyVisitsDelegate
    private val historyDocumentDelegate = lazy { HistoryDocumentController(applicationContext, object : HistoryDocumentController.Host {
        override fun current() = if (browserReady()) currentTab() else null
        override fun options(): HistoryDocumentOptions {
            val config = moe.pure.browser.feature.home.data.HomePreferences(this@BrowserActivity)
                .config(night = settings.night, isRtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL)
            return HistoryDocumentOptions(getString(R.string.ref_history_page_title), getString(R.string.ref_history_page_empty),
                getString(R.string.ref_history_page_incognito), config.isRtl, config.isNight, config.isLight, privateMode)
        }
        override fun navigate(tab: BrowserTab, url: String) { if (browserReady()) load(tab, url) }
        override fun append(url: String) { if (browserReady()) session.create(url, select = true) }
    }) }
    private val historyDocument by historyDocumentDelegate
    private val generatedPagesDelegate: Lazy<GeneratedPageController> = lazy { GeneratedPageController(this, object : GeneratedPageController.Host {
        override fun current() = if (browserReady()) currentTab() else null
        override fun navigate(tab: BrowserTab, url: String) { if (browserReady()) load(tab, url) }
        override fun append(url: String, adjacent: Boolean) {
            if (browserReady()) session.create(url, select = true, index = if (adjacent) selected + 1 else tabs.size)
        }
        override fun networkSource() = networkTools.generatedSource()
    }) }
    private val generatedPages: GeneratedPageController by generatedPagesDelegate
    private val configuredStartDelegate = lazy { ConfiguredStartPageController(this, object : ConfiguredStartPageController.Host {
        override fun current() = if (browserReady()) currentTab() else null
        override fun create(url: String, select: Boolean, index: Int): BrowserTab? =
            if (browserReady()) session.createResolvedStart(url, select, index) else null
        override fun navigate(original: BrowserTab, url: String) {
            if (!browserReady()) return
            original.navigateConfigured(url)
            if (original === currentTab()) refreshChrome()
        }
        override fun failed(error: Throwable) {
            moe.pure.browser.core.logging.NativeLog.warn("ConfiguredStartPage", "Resolve configured page", error)
            restoringSession = false
        }
    }) }
    private val configuredStart by configuredStartDelegate
    private val restoreNoticeDelegate = lazy { StartupRestoreNotice(this) }
    private val restoreNotice by restoreNoticeDelegate
    private val bookmarkPopupDelegate = lazy { BookmarkPopupController(this,
        { browserReady() && currentTab()?.liveView != null }, generatedPages::refreshCurrent) }
    private val bookmarkPopup by bookmarkPopupDelegate
    private val bookmarkDocumentActionsDelegate = lazy { BookmarkDocumentActions(this, generatedPages::refreshCurrent, bookmarkPopup) }
    private val bookmarkDocumentActions by bookmarkDocumentActionsDelegate
    private val savedDocumentActionsDelegate = lazy { SavedDocumentActions(this, generatedPages::refreshCurrent) }
    private val savedDocumentActions by savedDocumentActionsDelegate
    private val libraryDelegate = lazy { LibraryRepository(this) }
    private val library by libraryDelegate
    private val bookmarkChooserDelegate = lazy { BookmarksDialogController(this, object : BookmarksDialogController.Host {
        override fun open(url: String, newTab: Boolean) = search.submit(url, newTab = newTab)
        override fun openManager(folderId: String, position: Int, offset: Int) =
            LibraryNavigationEntry.open(this@BrowserActivity, LibraryTab.BOOKMARKS, folderId = folderId, position = position, offset = offset)
    }) }
    private val bookmarkChooser by bookmarkChooserDelegate
    private val savePageDelegate = lazy { SavePageController(this, { currentTab()?.liveView },
        { LibraryNavigationEntry.open(this, LibraryTab.OFFLINE) }) }
    private val savePage by savePageDelegate
    private val downloadsDelegate = lazy { DownloadController(this,
        chrome = { if (::chrome.isInitialized) chrome else null },
        panelManager = { rootHost.inlineManager }) }
    private val downloads by downloadsDelegate
    private val shortcutDelegate = lazy { moe.pure.browser.feature.shortcut.ShortcutController(this) }
    private val shortcuts by shortcutDelegate
    private val pickerDelegate = lazy { moe.pure.browser.feature.adblock.ElementPickerController(this, { chrome.content },
        { id -> tabs.firstOrNull { it.id == id }?.liveView }, { closeNativeChild() }) }
    private val picker by pickerDelegate
    private lateinit var find: FindInPageController
    private lateinit var undoClose: UndoCloseController
    private lateinit var pageInfo: PageInfoController
    private val qrCodeDelegate = lazy { QrCodeDialog(this, webPermissions::platform) }
    private val qrCode by qrCodeDelegate
    private val scriptsDelegate = lazy { moe.pure.browser.feature.scripts.ScriptController(this, { tabs }) }
    private val scripts by scriptsDelegate
    private val readerDelegate: Lazy<ReaderController> = lazy { ReaderController(this, ::currentTab,
        styled = { view, color -> currentTab()?.let { appearance.readerStyled(it, view, color) } },
        restored = { view -> currentTab()?.let { appearance.readerExited(it, view) } },
        openCssEditor = { moe.pure.browser.feature.reader.ui.ReaderCssNavigation.open(rootHost.fragment) { reader.appearance() } }) {
        if (::chrome.isInitialized) chrome.setReaderAvailable(reader.hintVisible(), reader.hintVisible())
    } }
    private val reader: ReaderController by readerDelegate
    private val readAloudDelegate = lazy { ReadAloudController(this) { chrome.content } }
    private val readAloud by readAloudDelegate
    private val translationDelegate = lazy { TranslationController(this, { chrome.content }, ::currentTab) { newTab(it) } }
    private val translation by translationDelegate
    private val homeSuggestionsDelegate = lazy { moe.pure.browser.feature.home.search.HomeSearchSuggestions(this) }
    private val homeSuggestions by homeSuggestionsDelegate
    private val gameMessages by lazy { moe.pure.browser.ui.components.BrowserMessage(this) }
    private val gameDelegate = lazy { GameModeController(this, {
        chrome.setGameMode(it)
        if (interactionsDelegate.isInitialized()) interactions.updateChromeControls()
        if (!it && homeDelegate.isInitialized()) home.rebind()
    }, { settings.fullscreen }, { gameMessages.show(it) }) }
    private val game by gameDelegate
    private val reportDelegate = lazy { moe.pure.browser.feature.report.ReportController(this) { currentTab()?.title } }
    private val reports by reportDelegate
    private val startupVersionDelegate = lazy { NativeStartupVersion(applicationContext) }
    private val startupVersion by startupVersionDelegate
    private val aiDelegate = lazy { moe.pure.browser.feature.ai.AiController(this, ::currentTab) }
    private val ai by aiDelegate
    private val searchDelegate: Lazy<SearchController> = lazy { SearchController(this, object : SearchController.Actions {
        override fun current() = currentTab()
        override fun tabs() = this@BrowserActivity.tabs
        override fun privateMode() = currentTab()?.let { webPolicy.isPrivate(it.url) } ?: this@BrowserActivity.privateMode
        override fun desktop() = siteRepository.resolve(currentTab()?.url.orEmpty(), SiteFeature.DESKTOP, settings) == moe.pure.browser.feature.site.SiteDecision.ALLOW
        override fun pageHost() = chrome.pageFocusHost
        override fun beforeOpen() {
            if (::find.isInitialized) find.close()
            if (::switcher.isInitialized) switcher.close()
            if (::pageInfo.isInitialized) pageInfo.close()
            if (downloadsDelegate.isInitialized()) downloads.close()
            if (pickerDelegate.isInitialized()) picker.close()
            if (translationDelegate.isInitialized()) translation.close()
            if (favoritesDelegate.isInitialized()) favorites.close()
        }
        override fun navigate(url: String, newTab: Boolean) = navigateInputUrl(url, newTab)
        override fun openMany(urls: List<String>) {
            val start = selected + 1
            urls.forEachIndexed { index, url -> session.create(url, select = index == urls.lastIndex, index = start + index) }
        }
        override fun switchTab(id: String) { tabs.indexOfFirst { it.id == id }.takeIf { it >= 0 }?.let(::showTab) }
        override fun toolbar(view: View?) { if (::chrome.isInitialized) chrome.setSearchToolbar(view) }
    }) }
    private val search by searchDelegate
    private val homeDelegate: Lazy<HomeController> = lazy { HomeController(this, object : HomeController.Actions {
        override fun current() = currentTab()
        override fun documentsChanged() {
            if (activityResumed && browserReady()) tabs.asReversed().forEach(generatedPages::refreshIfDirty)
        }
        override fun appearance(background: android.graphics.drawable.Drawable?, foreground: Int?) {
            val current = currentTab()
            if (current?.home == false && moe.pure.browser.core.BrowserInternalPages.kind(this@BrowserActivity, current.liveView?.url ?: current.url) <= 0 && appearanceDelegate.isInitialized()) appearance.refresh(current)
            else applyChromeAppearance(background, foreground)
        }
    }) }
    private val home: HomeController by homeDelegate
    private val appearanceDelegate: Lazy<PageAppearanceController> = lazy { PageAppearanceController(this, settings.preferences) { tab, color, foreground ->
        if (tab === currentTab() && !tab.home) {
            if (moe.pure.browser.core.BrowserInternalPages.kind(this, tab.liveView?.url ?: tab.url) > 0) home.selected(tab)
            else applyChromeAppearance(color?.let { android.graphics.drawable.ColorDrawable(it) }, foreground)
        }
    } }
    private val appearance: PageAppearanceController by appearanceDelegate
    private val favoritesDelegate = lazy { HomeFavoritesController(this, object : HomeFavoritesController.Actions {
        override fun current() = currentTab()
        override fun changed() { home.refresh() }
        override fun panelHeight(topCss: Int) = chrome.favoritePanelHeight(topCss)
    }) }
    private val favorites by favoritesDelegate
    private val passwordsDelegate = lazy { moe.pure.browser.feature.passwords.PasswordController(this, object : moe.pure.browser.feature.passwords.PasswordController.Actions {
        override fun current() = currentTab()
        override fun toolbar(view: View?) { if (::chrome.isInitialized) chrome.setPasswordToolbar(view) }
    }) }
    private val passwords by passwordsDelegate
    private val syncDelegate = lazy { moe.pure.browser.feature.sync.BrowserSyncController(this) {
        if (!isDestroyed && !isFinishing) {
            val currentNight = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (currentNight != palette.night) onBrowserNightChanged(settings.night)
            else { applyWebSettings(); applyWindowSettings(); home.refresh(); search.refresh(); refreshChrome() }
        }
    } }
    private val syncing by syncDelegate
    private val networkTools: NetworkToolsController by lazy { NetworkToolsController(this, { tabs }, generatedPages::refreshCurrent) { source, resources ->
        generatedPages.openNetwork(resources, source)
    } }
    private val customRules by lazy { CustomBlockRules(this) }
    private val adblock by lazy { moe.pure.browser.feature.adblock.AdblockEngine.get(this) }
    private val networkContext by lazy { NetworkContextMenu(this, customRules, object : NetworkContextMenu.Actions {
        override fun open(url: String, selected: Boolean) { session.create(url, select = selected) }
        override fun download(url: String) {
            val source = currentTab()?.let { current ->
                val id = moe.pure.browser.feature.network.NetworkLogPages.sourceId(this@BrowserActivity, current.url)
                tabs.firstOrNull { it.id == id } ?: current
            }
            if (source != null) downloads.request(source.webView, url, source.webView.settings.userAgentString, "attachment", null, -1, source.url)
            else downloads.confirm(url)
        }
        override fun share(url: String) = external.share(url)
        override fun play(url: String) = external.openMedia(url)
        override fun clear(tab: BrowserTab) = networkTools.clear(tab)
    }) }
    private val imageModeDelegate = lazy { PageImageModeController(this) }
    private val imageMode by imageModeDelegate
    private val pageContextDelegate = lazy { BrowserPageContextMenu(this, object : BrowserPageContextMenu.Host {
        override fun isCurrent(view: WebView) = currentTab()?.liveView === view
        override fun pageState(view: WebView) = ContextMenuPageState(
            enabled = !gameActive,
            customTab = customTabMode,
            javaScriptEnabled = view.settings.javaScriptEnabled,
            adBlockingEnabled = siteRepository.resolve(currentTab()?.url.orEmpty(), SiteFeature.ADBLOCK, settings) == moe.pure.browser.feature.site.SiteDecision.ALLOW,
            internalKind = moe.pure.browser.core.BrowserInternalPages.kind(this@BrowserActivity, view.url)
        )
        override fun deleteHistoryLink(view: WebView, url: String) = historyDocument.delete(url)
        override fun clearHistoryPage(view: WebView) = historyDocument.clear()
        override fun editBookmarkLink(view: WebView, url: String) = bookmarkDocumentActions.edit(url)
        override fun addBookmarkFavorite(view: WebView, url: String) = bookmarkDocumentActions.favorite(url)
        override fun deleteBookmarkLink(view: WebView, url: String) = bookmarkDocumentActions.delete(url)
        override fun editBookmarkFolder(view: WebView, folder: String) = bookmarkDocumentActions.editFolder(folder)
        override fun deleteBookmarkFolder(view: WebView, folder: String) = bookmarkDocumentActions.deleteFolder(folder)
        override fun deleteSavedPage(view: WebView, url: String) = savedDocumentActions.delete(url)
        override fun sortFavorites() = favorites.manage()
        override fun editFavorite(url: String) = favorites.add(url, null)
        override fun deleteFavorite(url: String) = favorites.delete(url)
        override fun shareLink(url: String) = external.share(url)
        override fun open(url: String, selected: Boolean) { session.create(url, select = selected) }
        override fun openText(text: String) = search.submit(text, newTab = true)
        override fun viewImage(url: String) {
            val key = moe.pure.browser.feature.site.NativeSiteKeys.fromUrl(url)
            val temporary = (siteRepository.getNative(key) ?: moe.pure.browser.feature.site.data.NativeSiteConfig())
                .withEnabled(true).withFeature(SiteFeature.IMAGES, moe.pure.browser.feature.site.SiteDecision.ALLOW)
            siteRepository.installTemporary(key, temporary)
            try { session.create(url, index = selected + 1) }
            finally { siteRepository.removeTemporary(key) }
        }
        override fun downloadImage(view: WebView, url: String) =
            downloads.request(view, url, view.settings.userAgentString, null, null, -1, view.url)
        override fun showImageMode() { currentTab()?.liveView?.let(imageMode::show) }
        override fun markAd() { currentTab()?.let(picker::show) }
        override fun customize() = SettingsEntry.open(this@BrowserActivity, moe.pure.browser.feature.settings.SettingsRoute.LONG_PRESS_MENU)
        override fun requestStoragePermission(onAllowed: () -> Unit) {
            if (Build.VERSION.SDK_INT >= 29) onAllowed()
            else webPermissions.platform(listOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) { granted ->
                if (android.Manifest.permission.WRITE_EXTERNAL_STORAGE in granted) onAllowed()
            }
        }
    }) }
    private val pageContext by pageContextDelegate
    private val closedTabsDelegate = lazy { ClosedTabsRestorer(applicationContext) }
    private val closedTabs by closedTabsDelegate
    private val clearData by lazy { ClearDataController(this, settings, { tabs.mapNotNull { it.liveView } }, session::clearClosed) }
    private val interactionsDelegate = lazy { BrowserInteractionController(this, settings, object : BrowserInteractionController.Host {
        override fun current() = currentTab()
        override fun tabCount() = tabs.size
        override fun selectedIndex() = selected
        override fun select(index: Int) = showTab(index)
        override fun newTab() { this@BrowserActivity.newTab() }
        override fun close(index: Int) = closeTab(index)
        override fun closeAll() { if (customTabMode) finish() else session.closeIndices(tabs.indices.toList()) }
        override fun duplicate() { currentTab()?.let(::duplicateTab) }
        override fun address() { chrome.setChromeVisible(true); search.show() }
        override fun newTabAndAddress() { requestConfiguredTab { address() } }
        override fun back() = navigateBack()
        override fun forward() = navigateForward()
        override fun bookmarks(dialog: Boolean) { if (dialog) showBookmarkChooser() else showSavedPages(false) }
        override fun history() = showSavedPages(true)
        override fun settings() = SettingsEntry.open(this@BrowserActivity)
        override fun perform(action: MenuAction) = performMenuAction(action, false)
        override fun chrome() = this@BrowserActivity.chrome
        override fun inputState() = BrowserInputState(
            browserVisible = browserStarted && !isFinishing && !isDestroyed && rootHost.isPresented,
            contentOverlayOpen = chrome.hasContentOverlay,
            panelOpen = chrome.hasContentOverlay,
            inputEditing = searchDelegate.isInitialized() && search.isShowing,
            keyboardVisible = chrome.keyboardVisible,
            gameMode = gameActive,
            customTab = customTabMode,
            chromeVisible = chrome.isChromeVisible,
            backAvailable = currentTab()?.let { it.canGoBack() || !moe.pure.browser.feature.home.HomeStartPageMatches.matches(this@BrowserActivity, it.url) } == true,
            forwardAvailable = session.forwardAvailable()
        )
        override fun dismissOverlay() = this@BrowserActivity.dismissOverlay()
        override fun showMenu() = this@BrowserActivity.showMenu()
        override fun showTabs() = this@BrowserActivity.showTabs()
        override fun goHome() = showHome()
        override fun restoreWindow() { if (this@BrowserActivity::chrome.isInitialized) applyWindowSettings() }
    }) }
    private val interactions by interactionsDelegate
    private val menuDelegate = lazy { BrowserMenuController(this, object : BrowserMenuController.Callbacks {
        override fun context(): MenuContext {
            val tab = currentTab()
            val url = tab?.liveView?.url ?: tab?.url.orEmpty()
            val imagePreferences = settings.preferences
            val imageMode = if (imagePreferences.wifiImages) 2 else if (imagePreferences.imagesEnabled) 0 else 1
            return MenuContext(tab?.home != false, url, settings.night, privateMode,
                store.isBookmarked(url), favorite = favorites.contains(url), desktop = settings.preferences.desktop,
                fullscreen = settings.fullscreen, imageMode = imageMode,
                customUserAgent = settings.userAgentId != 0, textZoom = settings.textZoom,
                orientation = settings.orientation, adblock = settings.preferences.adblock,
                game = gameActive,
                siteSettings = siteRepository.forUrl(url)?.enabled == true, scripts = tab?.let { scripts.pageHasScripts(it, url) } == true,
                reader = false,
                internalPage = tab == null || moe.pure.browser.core.BrowserInternalPages.isInternal(this@BrowserActivity, url),
                javascript = settings.preferences.javascript,
                orientationAdjustable = android.os.Build.VERSION.SDK_INT < 36 || resources.configuration.smallestScreenWidthDp < 600,
                parentWidth = chrome.content.measuredWidth, parentHeight = chrome.content.measuredHeight)
        }
        override fun perform(action: MenuAction, longPress: Boolean) = performMenuAction(action, longPress)
        override fun perform(action: MenuAction, longPress: Boolean, selected: Boolean) {
            if (action != MenuAction.READER) { performMenuAction(action, longPress); return }
            val tab = currentTab() ?: return
            if (longPress) {
                if (selected) pageInfo.showReader(tab) else reader.settings()
            } else if (selected) reader.exit() else reader.enter()
        }
        override fun exit() = requestExit()
    }) }
    private val menu by menuDelegate
    private val storeDelegate = lazy { BrowserStore(this) }
    private val store by storeDelegate
    private val databaseStartupDelegate = lazy { moe.pure.browser.data.BrowserDatabaseStartup(applicationContext) }
    private val databaseStartup by databaseStartupDelegate
    private val databaseWorker = Executors.newSingleThreadExecutor()
    private val networkObserverDelegate = lazy {
        moe.pure.browser.browser.BrowserNetworkObserver(this) { type, imagePolicyChanged ->
            if (browserReady()) {
                if (type != 0) moe.pure.browser.feature.download.DownloadCoordinator.get(applicationContext).requestNetworkResume()
                if (imagePolicyChanged) applyWebSettings()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        rootHost.prepare()
        GeneratedPageDirtyState.initialize(applicationContext)
        pendingBrowserState = savedInstanceState
        if (savedInstanceState != null) startRemoteSearchConfig()
        if (firstRun.showIfNeeded(customTabMode, savedInstanceState)) return
        startBrowser(savedInstanceState)
    }

    private fun startBrowser(savedInstanceState: Bundle?) {
        if (browserStarted || isFinishing || isDestroyed) return
        startRemoteSearchConfig()
        if (!databaseStartup.ready) {
            databaseStartup.prepare({ startBrowser(savedInstanceState) }) { error ->
                moe.pure.browser.core.logging.NativeLog.error("BrowserDatabase", "Cannot open browser database", error)
                toast("浏览器数据无法打开：${error.message}")
                finish()
            }
            return
        }
        if (engine.showIfUnavailable(savedInstanceState)) return
        browserStarted = true
        browserGeneration++
        initializeBrowser(savedInstanceState)
        networkObserverDelegate.value.start()
    }
    private fun startRemoteSearchConfig() {
        if (!customTabMode) remoteSearch.start()
    }
    private fun browserReady(generation: Long = browserGeneration) = browserStarted && generation == browserGeneration && !isFinishing && !isDestroyed

    private fun initializeBrowser(savedInstanceState: Bundle?) {
        NativeSkinIcons.adoptSelection(this)
        val generation = browserGeneration
        pendingFeatureState = savedInstanceState
        featuresRestored = false
        window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        NativeFullscreenState.apply(window, settings.fullscreen)
        applySystemBarAppearance()
        searchEntryConsumed = savedInstanceState?.getBoolean("search_entry_consumed", false) == true
        if (!searchEntryConsumed && searchEntry(intent)) pendingSearchIntent = intent

        external = BrowserExternalActions(this, webPermissions::externalApp)
        createChrome()
        webFactory = BrowserWebViewFactory(preferences, object : BrowserWebViewFactory.Callbacks {
            override fun interceptNavigation(view: WebView, url: String) = !browserReady() || redirects.shouldOverride(view, url)
            override fun clipboardDecision(url: String) = siteRepository.resolve(url, SiteFeature.CLIPBOARD, settings)
            override fun desktopDecision(url: String) = siteRepository.resolve(url, SiteFeature.DESKTOP, settings) == moe.pure.browser.feature.site.SiteDecision.ALLOW
            override fun readerProbe() { if (browserReady()) reader.probeCurrent() }
            override fun pageCommand(value: Int) = nativePageCommand(value)
            override fun pageControlsVisible() = ::chrome.isInitialized && chrome.nativeControlsVisible
            override fun pageAddon(payload: String) { if (browserReady()) scripts.addon(payload) }
            override fun pageMessage(value: moe.pure.browser.web.bridge.NativePageMessage) = nativePageMessage(value)
            override fun shouldOpenExternally(tab: BrowserTab, source: WebView, url: String): Boolean {
                return routePageUrl(tab, source, url)
            }
            override fun pageStarted(tab: BrowserTab, source: WebView, url: String, rawUrl: String) = onPageStarted(tab, source, url, rawUrl)
            override fun pageFinished(tab: BrowserTab, source: WebView, url: String) = onPageFinished(tab, source, url)
            override fun navigateHome(tab: BrowserTab, source: WebView, address: String) { if (browserReady()) navigateSource(tab, source, address) }
            override fun titleChanged(tab: BrowserTab, source: WebView, title: String?) {
                if (browserStarted && !isDestroyed) historyVisits.titleChanged(title, source.url)
                if (!browserReady()) return
                refreshPageMetadata(tab)
                if (!customTabMode && tab === currentTab() && tab.isCurrent(source) && source.isShown)
                    chrome.addressTitleChanged(title, source.url, settings.preferences.addressContent, effectiveAddressTabBar())
            }
            override fun urlChanged(tab: BrowserTab, source: WebView) = refreshPageMetadata(tab)
            override fun iconChanged(tab: BrowserTab, source: WebView) = refreshPageMetadata(tab)
            override fun progressChanged(tab: BrowserTab, source: WebView, progress: Int) {
                if (!browserReady()) return
                appearance.progress(tab, source, progress)
                if (!tab.isCurrent(source)) return
                if (tab === currentTab() && tab.liveView?.isShown == true) chrome.updateProgress(progress, tab.home)
            }
            override fun pageError(tab: BrowserTab, view: WebView, code: Int, description: String?, url: String?) = errors.onError(tab, view, code, description, url)
            override fun createPopup(tab: BrowserTab, source: WebView): WebView? = if (browserReady())
                session.create("", beforeStart = { it.preparePopup(tab.sourceUrl(source).orEmpty()) }).liveView else null
            override fun chooseFile(tab: BrowserTab, source: WebView, callback: android.webkit.ValueCallback<Array<android.net.Uri>>,
                params: WebChromeClient.FileChooserParams): Boolean = external.chooseFile(callback, params)
            override fun download(tab: BrowserTab, source: WebView, url: String, userAgent: String?, disposition: String?, mimeType: String?, length: Long) =
                downloads.request(source, url, userAgent, disposition, mimeType, length, tab.sourceUrl(source).orEmpty())
            override fun scriptOpen(url: String, active: Boolean, insert: Int) {
                if (browserReady() && url.isNotEmpty()) session.create(url, select = active, index = insert.takeIf { it in 0..tabs.size } ?: (selected + 1))
            }
            override fun scriptDownload(url: String, name: String) {
                if (browserReady()) downloads.fromScript(currentTab()?.liveView, url, name, agentRepository.resolve(settings, settings.preferences.desktop))
            }
            override fun pageDownload(tab: BrowserTab, source: WebView, value: moe.pure.browser.web.bridge.NativePageDownload) {
                if (!browserReady() || !tab.owns(source)) return
                if (pageContextDelegate.isInitialized() && pageContext.downloaded(source, value.url, value.data)) return
                downloads.fromPage(source, value.data, value.name, tab.sourceUrl(source))
            }
            override fun selectedText(tab: BrowserTab, source: WebView, text: String, find: Boolean) {
                if (tab !== currentTab() || !tab.isCurrent(source)) return
                if (find) this@BrowserActivity.find.show(source, text) else search.submit(text, newTab = true)
            }
            override fun httpAuthentication(tab: BrowserTab, view: WebView, handler: android.webkit.HttpAuthHandler, host: String) = passwords.httpAuthentication(tab, view, handler, host)
            override fun showCustomView(tab: BrowserTab, source: WebView, custom: View, callback: WebChromeClient.CustomViewCallback) = interactions.showCustomView(tab, source, custom, callback)
            override fun hideCustomView(tab: BrowserTab, source: WebView) = interactions.hideCustomView(source)
            override fun videoLoadingView() = interactions.videoLoadingView()
            override fun sslError(view: WebView, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError) = interactions.sslError(view, handler, error)
            override fun pageTouch(tab: BrowserTab, view: WebView, event: android.view.MotionEvent) = interactions.pageTouch(tab, view, event)
            override fun pageTouchDown(tab: BrowserTab, source: WebView, hasUrl: Boolean) { if (tab === currentTab() && tab.isCurrent(source)) interactions.input.onPageTouchDown(hasUrl) }
            override fun pageOverScrolled(tab: BrowserTab, source: WebView, clampedX: Boolean) { if (tab === currentTab() && tab.isCurrent(source)) interactions.input.onPageOverScrolled(clampedX) }
            override fun tabReleased(tab: BrowserTab, view: WebView) {
                if (interactionsDelegate.isInitialized()) interactions.released(tab, view)
                if (errorsDelegate.isInitialized()) errors.released(view)
                if (appearanceDelegate.isInitialized() && (tab.liveView === view || tab.liveView == null)) appearance.released(tab)
            }
            override fun javascriptDialog(view: WebView, kind: moe.pure.browser.web.dialog.JavaScriptDialogKind, url: String, message: String?, defaultValue: String?, result: android.webkit.JsResult) =
                interactions.javascriptDialog(view, kind, url, message, defaultValue, result)
            override fun formResubmission(view: WebView, cancel: android.os.Message, resend: android.os.Message) = interactions.formResubmission(view, cancel, resend)
            override fun closeWindow(tab: BrowserTab, source: WebView) { tabs.indexOf(tab).takeIf { it >= 0 }?.let(::closeTab) }
        }, webPermissions, blockRequest = { _, _, request, pageUrl ->
            !HomeDocument.isHome(this, pageUrl) &&
                siteRepository.resolve(pageUrl, SiteFeature.ADBLOCK, settings) == moe.pure.browser.feature.site.SiteDecision.ALLOW &&
                adblock.blocked(request, pageUrl)
        }, contextMenu = { tab, view -> if (gameActive) false else
            networkContext.show(tab, view) || pageContext.show(view) })

        moe.pure.browser.browser.startup.BrowserUpdateTasks.start(this)
        restoreTabs(savedInstanceState)
        if (!browserReady(generation)) return
        dispatchSearchEntry()
        restoreBrowserFeatures()
        if (activityResumed) resumeBrowser()
    }

    private fun restoreBrowserFeatures() {
        if (!browserReady() || featuresRestored || currentTab()?.liveView == null) return
        featuresRestored = true
        val generation = browserGeneration
        val state = pendingFeatureState
        pendingFeatureState = null
        state?.getBundle("downloads")?.let(downloads::restore)
        state?.getBundle("shortcut")?.let(shortcuts::restore)
        state?.getBundle("read_aloud")?.let(readAloud::restore)
        state?.getBundle("bookmark_chooser")?.let(bookmarkChooser::restore)
        scripts.bindRestored(currentTab())
        if (DownloadController.hasRestored(this)) downloads.rebind()
        if (BookmarkPopupController.hasRestored(this)) bookmarkPopup.rebind()
        if (moe.pure.browser.feature.download.ui.DownloadPageIntents.matches(intent)) {
            chrome.root.post { if (browserReady(generation)) consumeDownloadEntry(intent) }
        }
        if (intent.action == ReadAloudController.ACTION_SHOW || intent.dataString == ReadAloudController.INTERNAL_URL) readAloud.consume(intent)
        intent.dataString?.takeIf(TranslationController::isInternal)?.let { address ->
            intent.data = null; chrome.root.post { if (browserReady(generation)) translation.consume(address) }
        }
    }

    private fun createChrome() {
        val addressText = if (::chrome.isInitialized) chrome.captureAddressText() else null
        tabStrip?.dispose()
        tabStrip = null
        chrome = BrowserChrome(this, object : BrowserChrome.Actions {
            override fun onAddressEdit() = search.show()
            override fun onAddressMenu(anchor: View) = search.addressMenu(anchor)
            override fun onSearchEngine(anchor: View) = search.chooseEngine(anchor)
            override fun onBack() = navigateBack()
            override fun onForward() = navigateForward()
            override fun onHome() = showHome()
            override fun onTabs() = showTabs()
            override fun onMenu() = showMenu()
            override fun onReload() { currentTab()?.let { if (it.progress < 100) it.stop() else it.reload() } }
            override fun onScan() = performMenuAction(MenuAction.SCAN, false)
            override fun onPageInfo() {
                if (translationDelegate.isInitialized()) translation.close()
                if (downloadsDelegate.isInitialized()) downloads.close()
                if (pickerDelegate.isInitialized()) picker.close()
                currentTab()?.let { tab -> reader.addressClick { pageInfo.show(tab) } }
            }
            override fun onLongPress(binding: ActionBinding) = !customTabMode && interactions.input.onLongPress(binding)
            override fun toolbarHost() = if (customTabMode) android.widget.FrameLayout(this@BrowserActivity) else interactions.input.createToolbarLayout()
            override fun onInsetsChanged() { if (::chrome.isInitialized) interactions.updateChromeControls() }
        }, if (customTabMode) customTabs.toolbar else null, rootHost.inlineContainer)
        addressText?.let(chrome::restoreAddressText)
        find = FindInPageController(this, chrome.content)
        undoClose = UndoCloseController(this)
        pageInfo = PageInfoController(this, chrome.content, siteRepository, object : PageInfoController.Actions {
            override fun qrCode(url: String) = qrCode.show(url)
            override fun history(query: String) = LibraryNavigationEntry.open(this@BrowserActivity, LibraryTab.HISTORY, query)
            override fun readerState(completed: (Int) -> Unit) = reader.probe(completed)
            override fun readerOpen(tab: BrowserTab) = reader.enter()
            override fun readerClose(tab: BrowserTab) = reader.exit()
            override fun readerAppearance(tab: BrowserTab, close: () -> Unit, more: () -> Unit): View = reader.panel(close, more)
            override fun scriptsAvailable(tab: BrowserTab) = scripts.pageHasScripts(tab)
            override fun scriptsOpen(tab: BrowserTab) = scripts.show(tab, false)
        })
        switcher = TabSwitcher(this, chrome.content, object : TabSwitcher.Actions {
            override fun tabs(): List<BrowserTab> = session.tabs
            override fun selectedIndex(): Int = selected
            override fun select(index: Int) = showTab(index)
            override fun close(indices: List<Int>) = session.closeIndices(indices)
            override fun create() { newTab() }
            override fun closedTabs() { showClosedTabs() }
            override fun duplicate(index: Int) { session.tabs.getOrNull(index)?.let(::duplicateTab) }
            override fun move(from: Int, to: Int) = session.move(from, to)
        })
        rootHost.showBrowser(chrome)
        rebindChromeFeatures(rebindGestures = true)
    }

    private fun rebindChromeFeatures(rebindGestures: Boolean) {
        if (redirectsDelegate.isInitialized()) redirects.rebind()
        if (rebindGestures) interactions.rebind()
        if (downloadsDelegate.isInitialized()) downloads.rebind()
        if (pickerDelegate.isInitialized()) picker.rebind()
        if (readAloudDelegate.isInitialized()) readAloud.rebind()
        if (translationDelegate.isInitialized()) translation.rebind()
        if (gameDelegate.isInitialized() || gameActive) game.rebind()
        if (reportDelegate.isInitialized()) reports.updateGeometry()
        if (aiDelegate.isInitialized()) ai.rebind()
        if (searchDelegate.isInitialized()) search.rebind()
        if (homeDelegate.isInitialized()) home.rebind()
        if (passwordsDelegate.isInitialized()) passwords.rebind()
        if (bookmarkChooserDelegate.isInitialized()) bookmarkChooser.rebind()
    }

    private fun restoreTabs(state: Bundle?) {
        restoringSession = true
        val retained = lastCustomNonConfigurationInstance as? moe.pure.browser.browser.RetainedSession
        entries // Initialize the process document policy before any restored WebView is created.
        if (retained != null && retained.session.tabs.isNotEmpty()) {
            session.restore(retained.session, retained.privateHistoryIds, retained.runtime, preserveRuntime = true)
            finishSessionRestore(); return
        }
        if (customTabMode) {
            prepareEntry(intent.takeUnless { pendingSearchIntent != null }) { incoming ->
                if (incoming == null) { newTab("about:blank"); finishSessionRestore() }
                else openEntry(incoming, true) { finishSessionRestore() }
            }
        } else restoreSession(state)
    }

    private fun restoreSession(state: Bundle?) {
        if (!browserReady()) return
        val policy = settings.preferences.restoreTabs
        val forced = state?.getBoolean("restore_session", false) == true
        val pendingEntry = state?.getBoolean("entry_pending", false) == true
        session.readSaved(policy, forced, ConfiguredStartPageDocuments.prepare(this)) { saved ->
            if (!browserReady()) return@readSaved
            if (tabs.isNotEmpty()) { finishSessionRestore(); return@readSaved }
            val restored = saved.takeIf { it.tabs.isNotEmpty() && (forced || policy == 1) }
            if (restored != null) session.restore(restored, activate = false)
            else if (saved.tabs.isNotEmpty() && policy == 2) {
                restoreNotice.show(saved.tabs.mapNotNull { it.persistence.get() }, saved.selectedPersistentId) { ids, selectedId ->
                    val link = "v://tabs/restore?ids=" + ids.joinToString(",") + (selectedId?.let { "&selected=$it" } ?: "")
                    restoreClosedTabsLink(link)
                }
            }
            val incoming = intent.takeUnless { pendingSearchIntent != null || forced && !pendingEntry }
            prepareEntry(incoming) { entry ->
                val generation = browserGeneration
                startupVersion.apply(getString(R.string.ref_startup_custom)) {
                    if (browserReady(generation)) {
                        if (entry == null) finishStartupModels(restored, null)
                        else openEntry(entry, false) { opened -> finishStartupModels(restored, opened) }
                    }
                }
            }
        }
    }

    private fun finishStartupModels(restored: moe.pure.browser.browser.SavedSession?, incoming: BrowserTab?) {
        if (!browserReady()) return
        if (tabs.isEmpty()) {
            requestConfiguredTab(completed = ::finishSessionRestore)
            return
        }
        if (incoming != null) tabs.indexOf(incoming).takeIf { it >= 0 }?.let(session::select)
        else if (restored != null) session.activateRestored(restored)
        else if (currentTab() == null) session.select(tabs.lastIndex)
        session.save()
        finishSessionRestore()
    }

    private fun finishSessionRestore() {
        restoringSession = false
        if (!browserStarted) { queuedEntryIntent = null; return }
        queuedEntryIntent?.let { queued -> queuedEntryIntent = null; consumeIncomingIntent(queued) }
    }

    private fun searchEntry(incoming: Intent): Boolean = !BrowserEntryController.accepts(incoming) && SearchController.accepts(incoming)

    private fun prepareEntry(incoming: Intent?, completed: (BrowserEntry?) -> Unit) {
        val generation = browserGeneration
        val request = entries.parse(incoming)
        if (request == null) {
            if (incoming?.action == Intent.ACTION_VIEW && incoming.data?.scheme in setOf("file", "content")) toast("没有读取此文档的授权，或不支持此文档类型")
            completed(null); return
        }
        entries.prepare(request) { result ->
            if (!browserReady(generation)) return@prepare
            result.onFailure { toast(it.message ?: "无法打开文档") }
            completed(result.getOrNull())
        }
    }

    private fun openEntry(entry: BrowserEntry) { openEntry(entry, true) { } }

    private fun openEntry(entry: BrowserEntry, select: Boolean, completed: (BrowserTab?) -> Unit) {
        if (!browserStarted) return
        if (entry.routeOnly) {
            if (configuredStart.resolveInternal(entry.url) { url ->
                if (browserReady()) completed(session.create(url, select = select))
            }) return
            if (currentTab() == null) requestConfiguredTab { navigateInputUrl(entry.url); completed(currentTab()) }
            else { navigateInputUrl(entry.url); completed(currentTab()) }
            return
        }
        val existing = if (entry.reuseExisting) tabs.indexOfFirst { it.url == entry.url } else -1
        if (existing >= 0) { if (select) showTab(existing); completed(tabs[existing]); return }
        val tab = session.create(entry.url, select = false)
        if (select) session.select(tabs.indexOf(tab))
        completed(tab)
    }

    private fun requestConfiguredTab(select: Boolean = true, index: Int = tabs.size, completed: (() -> Unit)? = null) {
        configuredStart.create(select, index, completed)
    }
    private fun newTab() { requestConfiguredTab() }
    private fun newTab(url: String, select: Boolean = true): BrowserTab = session.create(url, select)
    private fun duplicateTab(source: BrowserTab) {
        val tab = session.duplicate(source, select = false)
        session.select(tabs.indexOf(tab))
    }
    private fun showTab(index: Int) = session.select(index)
    private fun currentTab(): BrowserTab? = session.current

    private fun renderTab(tab: BrowserTab) {
        if (!browserReady()) return
        if (pageContextDelegate.isInitialized()) pageContext.dismiss()
        interactions.selected(tab)
        if (passwordsDelegate.isInitialized()) passwords.selected()
        if (pickerDelegate.isInitialized()) picker.onSelected(tab)
        find.close()
        pageInfo.close()
        closeAddressInput()
        attachTab(tab)
        if (scriptsDelegate.isInitialized()) scripts.bindRestored(tab)
        dispatchSearchEntry()
        restoreBrowserFeatures()
    }

    private fun attachTab(tab: BrowserTab, publishAddress: Boolean = true, refreshDocument: Boolean = true) {
        chrome.showPage(tab.webView)
        home.selected(tab)
        if (refreshDocument) {
            historyDocument.refreshIfDirty(tab)
            generatedPages.refreshIfDirty(tab)
        }
        appearance.selected(tab)
        refreshChrome()
        if (publishAddress) publishSelectedAddress(tab)
        chrome.updateProgress(tab.liveView?.progress ?: tab.progress, tab.home)
    }

    private fun refreshChrome() {
        val tab = currentTab() ?: return
        chrome.setPage(tab.home, tab.url)
        chrome.setReaderAvailable(reader.hintVisible())
        chrome.setPageMask(pageNightMask(tab))
        refreshNavigation()
        if (searchDelegate.isInitialized()) search.refreshToolbar()
    }

    private fun refreshPageMetadata(tab: BrowserTab) {
        if (!browserReady()) return
        if (tab === currentTab()) refreshChrome() else refreshTabStrip()
        if (::switcher.isInitialized) switcher.refresh()
    }

    private fun effectiveAddressTabBar() = !customTabMode && settings.preferences.tabBar && chrome.tabStripAllowed

    private fun publishSelectedAddress(tab: BrowserTab) {
        val view = tab.liveView ?: return
        if (customTabMode) customTabs.pageChanged(moe.pure.browser.ui.chrome.address.NativeAddressDisplayUrl.unwrap(view.url).orEmpty())
        else chrome.addressSelected(view.title, view.url, settings.preferences.addressContent, effectiveAddressTabBar())
    }

    private fun refreshNavigation() {
        val tab = currentTab()
        chrome.updateNavigation(tab?.canGoBack() == true || tab?.let { !moe.pure.browser.feature.home.HomeStartPageMatches.matches(this, it.url) } == true,
            session.forwardAvailable(), tabs.size)
        refreshTabStrip()
    }

    private fun refreshTabStrip() {
        if (!::chrome.isInitialized) return
        val enabled = !customTabMode && settings.preferences.tabBar && chrome.tabStripAllowed
        if (!enabled) { chrome.setTabStrip(null); tabStrip?.dispose(); tabStrip = null; return }
        val strip = tabStrip ?: BrowserTabStrip(this, object : BrowserTabStrip.Actions {
            override fun select(index: Int) = showTab(index)
            override fun close(index: Int) = closeTab(index)
            override fun newTab() { this@BrowserActivity.newTab() }
            override fun move(from: Int, to: Int) = session.move(from, to)
            override fun closeIndices(indices: List<Int>) = session.closeIndices(indices)
            override fun duplicate(index: Int) { tabs.getOrNull(index)?.let(::duplicateTab) }
            override fun copyUrl(url: String) {
                getSystemService(android.content.ClipboardManager::class.java)
                    .setPrimaryClip(android.content.ClipData.newPlainText("网址", url))
            }
        }).also { tabStrip = it }
        chrome.setTabStrip(strip.view)
        strip.render(tabs, selected)
        recolorTabStrip()
    }

    private fun load(tab: BrowserTab, url: String) {
        val target = HomeDocument.normalize(this, url)
        tab.navigate(target)
        if (tab === currentTab()) refreshChrome()
    }

    private fun navigateSource(tab: BrowserTab, source: WebView, url: String) {
        if (tab.isCurrent(source)) load(tab, url)
        else if (tab.owns(source)) {
            val target = HomeDocument.normalize(this, url)
            applyWebSettings(tab, source, target)
            tab.urlChanged(source, target)
            source.loadUrl(target)
        }
    }

    private fun routePageUrl(tab: BrowserTab, source: WebView, url: String): Boolean {
        val homeTarget = HomeDocument.normalize(this, url)
        if (homeTarget != url || HomeDocument.isHome(this, url) && !HomeDocument.isHome(this, tab.sourceUrl(source))) {
            navigateSource(tab, source, homeTarget); return true
        }
        if (errors.consumeJump(tab, source, url)) return true
        val favoriteTarget = HomeFavoritesController.unwrap(url)
        if (favoriteTarget != url) { navigateInputUrl(favoriteTarget); return true }
        if (SearchController.isSearchUrl(url)) { search.consume(url); return true }
        if (url == ReadAloudController.INTERNAL_URL) { readAloud.show(); return true }
        if (TranslationController.isInternal(url)) { translation.consume(url); return true }
        if (url.startsWith("history://", ignoreCase = true) || url.startsWith("folder://", ignoreCase = true) ||
            GeneratedAddress.isV(GeneratedAddress.normalize(url))) {
            navigateInputUrl(url); return true
        }
        val uri = runCatching { android.net.Uri.parse(url) }.getOrNull()
        if (uri?.scheme in listOf("v", "via") && uri?.host in setOf("bookmarks", "history", "tabs", "downloader", "scanner")) {
            navigateInputUrl(url); return true
        }
        if (uri?.scheme in setOf("folder", "history")) { navigateInputUrl(url); return true }
        if (uri?.scheme in setOf("v", "via")) return true
        return external.handleNavigation(url, tab.sourceUrl(source).orEmpty())
    }

    private fun closeAddressInput() { if (searchDelegate.isInitialized()) search.close(false) }

    private fun navigateInputUrl(input: String, createTab: Boolean = false) {
        val url = GeneratedAddress.normalize(input)
        val currentView = currentTab()?.liveView
        val currentOwner = currentTab()
        if (currentOwner != null && currentView != null && errors.consumeJump(currentOwner, currentView, url)) return
        if (url.startsWith("v://blocker/jump?url=", ignoreCase = true)) {
            val target = moe.pure.browser.feature.adblock.AdblockNavigationState.target(android.net.Uri.parse(url))
            if (target != null) {
                currentOwner?.let { moe.pure.browser.feature.adblock.AdblockNavigationState.of(it).allow(target) }
                chrome.root.post { if (browserReady()) currentTab()?.let { load(it, target) } }
            }
            return
        }
        if (SearchController.isSearchUrl(url)) { search.consume(url); return }
        if (url == ReadAloudController.INTERNAL_URL) { readAloud.show(); return }
        if (TranslationController.isInternal(url)) { translation.consume(url); return }
        if (url.startsWith("javascript:", true)) { currentTab()?.liveView?.loadUrl(url); return }
        if (url.startsWith("history://", ignoreCase = true)) { historyDocument.open(); return }
        if (url.startsWith("folder://", ignoreCase = true)) {
            val argument = url.substring(9)
            val current = currentTab()
            if (moe.pure.browser.core.BrowserInternalPages.kind(this, current?.liveView?.url ?: current?.url) in setOf(2, 11)) {
                generatedPages.open(if (argument.isEmpty()) 2 else 11, argument)
            } else {
                val folderId = library.findFolderByTitle(android.net.Uri.decode(argument))?.id.orEmpty()
                LibraryNavigationEntry.open(this, LibraryTab.BOOKMARKS, folderId = folderId)
            }
            return
        }
        val homeTarget = HomeDocument.normalize(this, url)
        if (homeTarget != url) {
            val current = currentTab()
            if (createTab || current == null) newTab(homeTarget) else load(current, homeTarget)
            return
        }
        if (GeneratedAddress.isV(url)) {
            when {
                GeneratedAddress.matches(url, "v://bookmarks") -> openBookmarksAlias(GeneratedAddress.folder(url))
                GeneratedAddress.matches(url, "v://history") -> openHistoryAlias()
                GeneratedAddress.matches(url, "v://scanner") -> performMenuAction(MenuAction.SCAN, false)
                GeneratedAddress.matches(url, "v://downloader") -> openDownloads()
                GeneratedAddress.matches(url, "v://readaloud") -> readAloud.show()
                GeneratedAddress.matches(url, "v://search") -> search.consume("v://search" + url.substring(10))
                GeneratedAddress.matches(url, "v://skins") -> SettingsEntry.open(this, moe.pure.browser.feature.settings.SettingsRoute.SKINS)
                url.startsWith("v://tabs", ignoreCase = true) -> restoreClosedTabsLink(url)
                else -> when (val kind = GeneratedAddress.kind(url)) {
                    1 -> currentTab()?.takeUnless { moe.pure.browser.feature.home.HomeStartPageMatches.matches(this, it.liveView?.url ?: it.url) }?.let { showHome() }
                    3 -> historyDocument.open()
                    else -> generatedPages.open(kind)
                }
            }
            return
        }
        val current = currentTab()
        if (external.handleNavigation(url, current?.url.orEmpty())) return
        if (createTab || current == null) newTab(url) else load(current, url)
    }

    private fun dispatchSearchEntry() {
        if (!browserReady()) return
        val incoming = pendingSearchIntent ?: return
        if (currentTab()?.liveView == null) return
        pendingSearchIntent = null
        searchEntryConsumed = search.consume(incoming)
    }

    private fun onPageStarted(tab: BrowserTab, source: WebView, url: String, rawUrl: String) {
        if (!browserStarted || isDestroyed || isFinishing) return
        errors.navigation(tab, source)
        scripts.pageStarted(url)
        if (pickerDelegate.isInitialized()) picker.navigation(source)
        appearance.navigation(tab, source, url)
        if (webPolicy.isPrivate(url)) tab.hasPrivateHistory = true
        if (!tab.isCurrent(source)) return
        interactions.navigation(tab)
        if (tab === currentTab() && pageContextDelegate.isInitialized()) pageContext.dismiss()
        home.navigation(tab)
        passwords.navigation(tab)
        reader.navigation(tab)
        if (tab === currentTab()) {
            pageInfo.close()
            chrome.setPage(tab.home, url)
            if (source.isShown) {
                if (customTabMode) customTabs.pageChanged(moe.pure.browser.ui.chrome.address.NativeAddressDisplayUrl.unwrap(rawUrl).orEmpty())
                else chrome.addressUrlChanged(rawUrl, settings.preferences.addressContent, effectiveAddressTabBar())
            }
            chrome.setPageMask(pageNightMask(tab))
        }
    }

    private fun onPageFinished(tab: BrowserTab, source: WebView, url: String) {
        if (!browserStarted || isDestroyed || isFinishing) return
        session.checkpoint(tab)
        appearance.finished(tab, source, url)
        if (tab.isCurrent(source)) {
            if (tab === currentTab()) refreshChrome()
        }
    }

    private fun handleBack() {
        if (interactions.back() || dismissOverlay()) return
        val tab = currentTab() ?: return
        if (customTabMode) {
            if (tab.canGoBack()) tab.goBack()
            else if (tabs.size > 1) closeTab(selected) else finish()
        } else if (!moe.pure.browser.feature.home.HomeStartPageMatches.matches(this, tab.url)) navigateBack()
        else if (tabs.size > 1) closeTab(selected)
        else {
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastSystemBackAt <= 1500L) requestExit()
            else {
                moe.pure.browser.ui.components.BrowserMessage(this).show(getString(R.string.ref_tab_back_exit))
                lastSystemBackAt = now
            }
        }
    }

    private fun dismissOverlay(): Boolean {
        if (bookmarkChooserDelegate.isInitialized() && bookmarkChooser.back()) return true
        if (customTabsDelegate.isInitialized() && customTabs.close()) return true
        if (menuDelegate.isInitialized() && menu.close()) return true
        if (passwordsDelegate.isInitialized() && passwords.back()) return true
        if (favoritesDelegate.isInitialized() && favorites.back()) return true
        if (searchDelegate.isInitialized() && search.back()) return true
        if (translationDelegate.isInitialized() && translation.close()) return true
        if (pickerDelegate.isInitialized() && picker.close()) return true
        if (downloadsDelegate.isInitialized() && downloads.back()) return true
        if (::pageInfo.isInitialized && pageInfo.close()) return true
        if (::switcher.isInitialized && switcher.close()) return true
        if (::find.isInitialized && find.close()) return true
        return gameActive && game.onBack()
    }

    private fun navigateBack() {
        val tab = currentTab() ?: return
        if (tab.canGoBack()) {
            if (tab.progress < 100) tab.stop()
            tab.goBack()
        } else if (!moe.pure.browser.feature.home.HomeStartPageMatches.matches(this, tab.url)) closeTab(selected)
    }

    private fun navigateForward() {
        session.goForward()
    }

    private fun showHome() {
        closeAddressInput()
        configuredStart.home()
    }

    private fun closeTab(index: Int) { if (customTabMode && tabs.size == 1 && index in tabs.indices) finish() else session.close(index) }

    private fun showTabs() {
        closeAddressInput()
        if (translationDelegate.isInitialized()) translation.close()
        if (pickerDelegate.isInitialized()) picker.close()
        if (downloadsDelegate.isInitialized()) downloads.close()
        pageInfo.close(); switcher.show()
    }

    private fun showMenu() {
        if (customTabMode) { customTabs.menu(); return }
        closeAddressInput()
        if (translationDelegate.isInitialized()) translation.close()
        if (pickerDelegate.isInitialized()) picker.close()
        if (downloadsDelegate.isInitialized()) downloads.close()
        pageInfo.close()
        switcher.close()
        currentTab()?.let { tab ->
            menu.show()
            if (menu.isShowing) reader.probe(menu.readerStateCallback())
        }
    }

    private fun shareCurrentPage(tab: BrowserTab) {
        val view = tab.liveView
        external.sharePage(if (view == null) tab.url else view.url, if (view == null) tab.title else view.title)
    }

    private fun performMenuAction(action: MenuAction, longPress: Boolean) {
        val tab = currentTab() ?: return
        when (action) {
            MenuAction.BOOKMARKS -> if (longPress) showBookmarkChooser() else showSavedPages(false)
            MenuAction.HISTORY -> if (longPress) historyDocument.open() else showSavedPages(true)
            MenuAction.OFFLINE -> LibraryNavigationEntry.open(this, LibraryTab.OFFLINE)
            MenuAction.SAVE -> savePage.show(tab.webView)
            MenuAction.SOURCE -> if (tab.home) moe.pure.browser.data.BrowserExecutors.persistence.execute {
                val source = runCatching { HomeDocument.sourceUrl(this) }
                runOnUiThread { if (!isDestroyed) source.onSuccess { newTab("view-source:$it") }.onFailure { toast("无法读取主页源码") } }
            } else newTab("view-source:" + tab.url)
            MenuAction.NETWORK_LOG -> networkTools.show(tab, false)
            MenuAction.RESOURCES -> networkTools.show(tab, true)
            MenuAction.MARK_AD -> picker.show(tab)
            MenuAction.SCRIPTS -> scripts.show(tab, longPress)
            MenuAction.READER -> if (longPress) {
                reader.probe { mode -> if (mode == 3) currentTab()?.let(pageInfo::showReader) else reader.settings() }
            } else reader.toggle()
            MenuAction.READ_ALOUD -> if (longPress) readAloud.show() else readAloud.start(tab)
            MenuAction.TRANSLATE -> if (longPress) translation.showProviders() else translation.translatePage(tab)
            MenuAction.GAME -> if (!longPress) game.toggle()
            MenuAction.REPORT -> if (!longPress) reports.show(tab.url)
            MenuAction.AI -> if (longPress) ai.settings() else ai.show()
            MenuAction.DOWNLOADS -> if (longPress) downloads.chooseManager() else openDownloads()
            MenuAction.INCOGNITO -> toggleIncognito()
            MenuAction.SHARE -> shareCurrentPage(tab)
            MenuAction.ADD_BOOKMARK -> toggleBookmark(tab)
            MenuAction.ADD_FAVORITE -> {
                val internalPage = tab.home || tab.url == "about:blank" || moe.pure.browser.feature.network.NetworkLogPages.isPage(this, tab.url)
                favorites.add(if (internalPage) "https://" else tab.url, if (internalPage) "" else tab.title)
            }
            MenuAction.ADD_SHORTCUT -> shortcuts.show(if (tab.home) "" else tab.url, if (tab.home) "Via" else tab.title, tab.liveView?.favicon)
            MenuAction.DESKTOP -> toggleDesktop(tab)
            MenuAction.FIND -> find.show(tab.webView)
            MenuAction.FONT_SIZE -> currentTab()?.let { pageFont.show(it.url) }
            MenuAction.ORIENTATION -> tools.orientation(::refreshChrome)
            MenuAction.USER_AGENT -> UserAgentDialogs(this, agentRepository, settings).choose(::userAgentChanged)
            MenuAction.SITE_SETTINGS -> SiteSettingsNavigation.open(this, if (tab.home || longPress) null else tab.url)
            MenuAction.ADBLOCK -> if (longPress) moe.pure.browser.feature.adblock.ui.AdblockNavigation.settings(this) else {
                val enabled = !settings.preferences.adblock
                settings.preferences.adblock = enabled
                if (!tab.home) tab.reload()
                toast("广告拦截 ${if (enabled) "已开启" else "已关闭"}")
            }
            MenuAction.CLEAR_DATA -> clearData.show()
            MenuAction.PRINT -> tools.print(tab.webView)
            MenuAction.SCAN -> moe.pure.browser.feature.scan.ScannerNavigation.open(this)
            MenuAction.FULLSCREEN -> chrome.root.postDelayed({
                settings.fullscreen = !settings.fullscreen
                applyWindowSettings()
            }, 100L)
            MenuAction.IMAGES -> tools.images { applyWebSettings(); refreshChrome() }
            MenuAction.NIGHT -> {
                val change = moe.pure.browser.ui.theme.BrowserNightMode.toggle(this)
                onBrowserNightChanged(change.night)
                if (change.showFollowingSystemNotice) toast(getString(R.string.ref_night_follow_system))
            }
            MenuAction.RELOAD -> tab.reload()
            MenuAction.OPEN_WITH -> external.openWith(tab.webView.url.orEmpty())
            MenuAction.TOOLS -> { showMenu(); menu.nextPage() }
            MenuAction.SETTINGS -> if (longPress) moe.pure.browser.feature.menu.customize.MenuCustomizationEntry.open(this)
                else SettingsEntry.open(this)
            MenuAction.CUSTOMIZE -> moe.pure.browser.feature.menu.customize.MenuCustomizationEntry.open(this)
        }
    }

    private fun applyWindowSettings() {
        if (interactionsDelegate.isInitialized() && interactions.customViewShowing) return
        NativeFullscreenState.apply(window, settings.fullscreen)
        settings.applyOrientation(this)
        if (::chrome.isInitialized) {
            chrome.refreshViewportPolicy()
            chrome.root.requestApplyInsets()
        }
        if (gameActive) game.onResume()
    }
    private fun applyChromeAppearance(background: android.graphics.drawable.Drawable?, foreground: Int?) {
        chromeForeground = foreground
        if (::chrome.isInitialized) chrome.setAppearance(background, foreground)
        if (customTabMode && customTabsDelegate.isInitialized()) customTabs.recolor((background as? android.graphics.drawable.ColorDrawable)?.color, foreground)
        recolorTabStrip()
        if (!gameActive && !(interactionsDelegate.isInitialized() && interactions.customViewShowing)) updateHomepageSystemBars(foreground)
    }

    /** Repaint existing chrome without detaching its live WebView or restarting progress/gestures. */
    override fun onBrowserNightChanged(night: Boolean): Boolean {
        if (!browserStarted || !::chrome.isInitialized) return false
        applyBrowserNightTheme()
        GeneratedPageDirtyState.markAll()
        if (pageContextDelegate.isInitialized()) pageContext.dismiss()
        find.refreshTheme(); undoClose.close(); switcher.close(animate = false); pageInfo.close()
        chrome.root.animate().cancel()
        chrome.refreshTheme()
        refreshChrome()
        rebindChromeFeatures(rebindGestures = false)
        applyWindowSettings()
        tabs.asReversed().forEach { historyDocument.refreshIfDirty(it); generatedPages.refreshIfDirty(it) }
        applyWebSettings()
        currentTab()?.let { appearance.themeChanged(it) }
        reader.consumePageEffects()
        if (!settings.preferences.forceDark || Build.VERSION.SDK_INT < 29)
            chrome.pageFocusHost.dispatchConfigurationChanged(resources.configuration)
        if (activityResumed && chrome.root.isShown) {
            chrome.root.alpha = .2f
            chrome.root.animate().alpha(1f).setDuration(300L)
                .setInterpolator(android.view.animation.AccelerateInterpolator()).start()
        }
        return true
    }

    private fun pageNightMask(tab: BrowserTab): Int = moe.pure.browser.web.compat.WebDarkMode.currentMask(
        settings.night, tab.home || moe.pure.browser.core.BrowserInternalPages.kind(this, tab.liveView?.url ?: tab.url) > 0,
        settings.preferences.nightFilter)
    private fun recolorTabStrip() {
        val foreground = chromeForeground
        val text = when (foreground) { null -> palette.text; 0xFF232323.toInt() -> 0xDE000000.toInt(); else -> foreground }
        tabStrip?.recolor(foreground ?: palette.icon, text)
    }
    private fun updateHomepageSystemBars(foreground: Int?) {
        if (hasRecordPage) return
        val light = foreground?.let { Color.red(it) < 128 } ?: !palette.night
        NativeSystemBarAppearance.light(window, light)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = if (foreground == null) palette.canvas else Color.TRANSPARENT
    }
    private fun openDownloads(forceBuiltin: Boolean = false) {
        if (translationDelegate.isInitialized()) translation.close()
        pageInfo.close(); switcher.close(); downloads.show(forceBuiltin)
    }

    private fun configureWebView(tab: BrowserTab, view: WebView) {
        webFactory.configure(tab, view)
    }

    private fun onScannerResult(result: String) {
        if (currentTab() != null) navigateInputUrl(moe.pure.browser.feature.scan.ScanResultNavigation.destination(this, result))
    }

    private fun onLibraryResult(result: Bundle?) {
        if (bookmarkChooserDelegate.isInitialized()) bookmarkChooser.rebind()
        for (request in LibraryOpenResult.read(result).orEmpty()) {
            if (!browserReady()) break
            when (request.where) {
                LibraryOpenWhere.CURRENT -> navigateInputUrl(request.url)
                LibraryOpenWhere.FOREGROUND -> newTab(request.url)
                LibraryOpenWhere.BACKGROUND -> newTab(request.url, select = false)
                LibraryOpenWhere.FORCE_CURRENT -> currentTab()?.let { load(it, request.url) } ?: newTab(request.url)
            }
        }
    }
    private fun showClosedTabs() {
        moe.pure.browser.ui.tabs.closed.ClosedTabsNavigation.open(rootHost.fragment) { result ->
            result.getStringArray("tabs")?.takeIf { it.isNotEmpty() }?.let { ids ->
                closedTabs.restore(ids.toList(), result.getString("selected"), tabs.size, selected, { session.reopen(it, false) }, ::showTab)
            }
        }
    }
    private fun restoreClosedTabsLink(url: String) {
        closedTabs.restoreLink(url, tabs.size, selected, { session.reopen(it, false) }, ::showTab)
    }

    private fun onSiteSettingsResult(changed: Int) {
        if (changed and 2 != 0) currentTab()?.let { pageFont.show(it.url, fromSite = true, returnFlags = changed) }
        else if (changed and 1 != 0) currentTab()?.let { tab ->
            tab.liveView?.let { applyWebSettings(tab, it) }; tab.reload()
        }
    }

    private fun applyWebSettings() {
        if (currentTab() == null) return
        tabs.forEach { tab ->
            if (webPolicy.isPrivate(tab.url)) tab.hasPrivateHistory = true
            tab.liveView?.let { applyWebSettings(tab, it) }
            tab.liveView?.let { moe.pure.browser.web.compat.WebDarkMode.synchronizePage(it,
                it.url ?: tab.url, settings.night, settings.preferences.forceDark) }
        }
        WebView.setWebContentsDebuggingEnabled(settings.preferences.webDebugging)
        policyFingerprint = webPolicy.reloadFingerprint()
        GeneratedPageDirtyState.clear(GeneratedPageDirtyState.WEB_SETTINGS)
    }

    private fun applyWebSettings(tab: BrowserTab, view: WebView, pageUrl: String = tab.sourceUrl(view) ?: tab.url) {
        webPolicy.apply(tab, view, palette.canvas, pageUrl)
        webFactory.pageSettingsChanged(view)
    }

    private fun userAgentChanged() {
        applyWebSettings()
        currentTab()?.let { tab ->
            val authority = moe.pure.browser.feature.site.NativeSiteKeys.popupDestination(tab.url)
            if (!moe.pure.browser.feature.home.HomeStartPageMatches.matches(this, tab.url) && authority.isNotEmpty() &&
                listOf("pan.baidu.com", "yun.baidu.com", "eyun.baidu.com").none { it.equals(authority, ignoreCase = true) }) tab.reload()
        }
        refreshChrome()
    }

    private fun toggleBookmark(tab: BrowserTab) {
        val url = tab.url
        if (moe.pure.browser.core.BrowserInternalPages.kind(this, url) > 0) bookmarkPopup.show("https://", "")
        else bookmarkPopup.show(url, tab.title)
    }

    private fun nativePageCommand(value: Int) {
        if (!browserReady()) return
        when (value) {
            257 -> showSavedPages(false)
            514 -> if (supportFragmentManager.backStackEntryCount == 0) chrome.startAddressEdit()
            516 -> currentTab()?.liveView?.let(tools::print)
            517 -> {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastPageMarkerAt < 300) return
                lastPageMarkerAt = now
                if (!rootHost.isPresented || closeNativeChild()) return
                currentTab()?.let(picker::show)
            }
        }
    }

    private fun closeNativeChild(): Boolean {
        if (downloadsDelegate.isInitialized() && downloads.close()) return true
        return pickerDelegate.isInitialized() && picker.close()
    }

    private fun nativePageMessage(value: NativePageMessage) {
        if (!browserReady()) return
        when (value) {
            is NativePageMessage.Phase -> tabs.firstNotNullOfOrNull { it.liveView?.takeIf { page -> page.id == value.from } }
                ?.let { webFactory.pagePhase(it, value.runAt) }
            is NativePageMessage.MarkerFilter -> if (pickerDelegate.isInitialized()) picker.update(value.filter)
            is NativePageMessage.MarkerSave -> picker.confirm(value.host, value.filter)
            is NativePageMessage.Submit -> { search.submit(value.text); closeNativeChild() }
            is NativePageMessage.Suggest -> homeSuggestions.query(value.text) { words ->
                val values = org.json.JSONArray(words).toString().replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
                currentTab()?.liveView?.evaluateJavascript("try{OpenSuggestion.pushSuggestions($values)}catch(ignored){}", null)
            }
            is NativePageMessage.Download -> {
                downloads.fromPageUrl(currentTab()?.liveView, value.url)
                translation.showProviders(false)
            }
            NativePageMessage.Translation -> translation.showProviders(false)
            is NativePageMessage.PasswordFocus -> passwords.focused(currentTab(), value.show)
            is NativePageMessage.PasswordCapture -> passwords.captured(value.url, value.username, value.password)
        }
    }

    private fun showSavedPages(history: Boolean) {
        LibraryNavigationEntry.open(this, if (history) LibraryTab.HISTORY else LibraryTab.BOOKMARKS)
    }

    private fun openHistoryAlias() {
        val tab = currentTab()
        if (moe.pure.browser.core.BrowserInternalPages.kind(this, tab?.liveView?.url ?: tab?.url) == 5) historyDocument.open()
        else showSavedPages(true)
    }
    private fun openBookmarksAlias(folder: String?) {
        val requested = folder?.takeIf { it.isNotEmpty() }
        val tab = currentTab()
        when (moe.pure.browser.core.BrowserInternalPages.kind(this, tab?.liveView?.url ?: tab?.url)) {
            2, 5, 11 -> generatedPages.open(if (requested == null) 2 else 11, requested)
            1 -> bookmarkChooser.show(requested)
            else -> LibraryNavigationEntry.open(this, LibraryTab.BOOKMARKS, folderId = requested.orEmpty())
        }
    }
    private fun showBookmarkChooser() {
        closeAddressInput()
        if (translationDelegate.isInitialized()) translation.close()
        if (downloadsDelegate.isInitialized()) downloads.close()
        if (pickerDelegate.isInitialized()) picker.close()
        pageInfo.close(); switcher.close()
        bookmarkChooser.show()
    }

    private fun toggleDesktop(tab: BrowserTab) {
        val enabled = !settings.preferences.desktop
        moe.pure.browser.ui.components.BrowserMessage(this).show(getString(
            if (enabled) R.string.ref_desktop_enabled else R.string.ref_desktop_disabled,
            getString(R.string.ref_desktop_mode)))
        settings.preferences.desktop = enabled
        applyWebSettings()
        if (moe.pure.browser.core.BrowserInternalPages.isInternal(this, tab.url)) return
        val converted = moe.pure.browser.feature.useragent.desktop.DesktopUrlRewriter.rewrite(tab.url,
            enabled || moe.pure.browser.feature.useragent.desktop.DesktopUrlRewriter.tablet(this))
        if (converted.isNullOrEmpty()) tab.reload()
        else {
            moe.pure.browser.feature.appearance.PageColorCache.invalidate(converted)
            tab.navigate(converted)
        }
    }

    private fun toggleIncognito() {
        fun setMode(enabled: Boolean) {
            undoClose.close()
            settings.preferences.incognito = enabled
            HistoryDirtyState.markDirty()
            applyWebSettings()
            if (!enabled) session.save()
            toast(if (enabled) "已进入隐身浏览" else "退出隐身浏览")
        }
        if (!privateMode) { session.save(); setMode(true) }
        else if (tabs.size > 1 || currentTab()?.home == false) {
            dialogScope.show(moe.pure.browser.ui.dialog.BrowserDialog(this,
                getString(R.string.ref_incognito_exit_title), getString(R.string.ref_incognito_exit_message))
                .positive(getString(R.string.ref_incognito_exit_close)) { session.closeIndices(tabs.indices.toList()); setMode(false); true }
                .negative(getString(R.string.ref_incognito_exit_keep)) { setMode(false); true })
        } else setMode(false)
    }

    private fun toast(message: String) { moe.pure.browser.ui.message.BrowserMessages.show(this, message) }

    private fun requestExit() {
        if (customTabMode) { finish(); return }
        fun exit() {
            session.save()
            clearData.clear(settings.preferences.exitClearMask, exiting = true)
            finish()
        }
        if (settings.preferences.confirmExit) {
            dialogScope.show(moe.pure.browser.ui.dialog.BrowserDialog(this, "退出", "确定退出浏览器？")
                .positive("确定") { exit(); true }.negative())
        } else exit()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        (!hasActiveRecordInput && browserStarted && interactionsDelegate.isInitialized() && interactions.key(event)) || super.dispatchKeyEvent(event)

    override fun onSystemBack() {
        if (!browserStarted) {
            if (firstRunDelegate.isInitialized() && firstRun.back()) return
            if (engineDelegate.isInitialized() && engine.isShowing) { finish(); return }
            finish(); return
        }
        handleBack()
    }

    internal override fun beginBrowserPredictiveBack(): PredictiveBackPreview? {
        if (!browserReady() || !rootHost.isPresented || !::chrome.isInitialized || chrome.hasContentOverlay ||
            searchDelegate.isInitialized() && search.isShowing || menuDelegate.isInitialized() && menu.isShowing) return null
        val tab = currentTab()?.takeIf { it.canGoBack() } ?: return null
        val page = chrome.pageFocusHost.takeIf { it.isShown && it.width > 0 } ?: return null
        return WebBackPreview(page) { if (browserReady() && currentTab() === tab) navigateBack() }
    }

    override fun onRecordPageVisibilityChanged(showing: Boolean) {
        if (showing || !browserStarted || !::chrome.isInitialized) super.onRecordPageVisibilityChanged(showing)
        else updateHomepageSystemBars(chromeForeground)
    }

    private fun onBrowserRootVisibilityChanged(visible: Boolean) {
        if (!browserStarted || !::chrome.isInitialized) return
        if (downloadsDelegate.isInitialized()) downloads.hostVisibilityChanged(visible)
        if (interactionsDelegate.isInitialized()) interactions.updateChromeControls()
        if (visible && sessionDelegate.isInitialized() && currentTab() != null) {
            chrome.root.post { if (rootHost.isPresented) resumeBrowser() }
        }
    }

    @Deprecated("Used by the framework file picker")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (!browserStarted) return
        if (passwordsDelegate.isInitialized() && passwords.result(requestCode, resultCode, data)) return
        if (shortcutDelegate.isInitialized() && shortcuts.onActivityResult(requestCode, resultCode, data)) return
        external.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!browserStarted) { super.onRequestPermissionsResult(requestCode, permissions, grantResults); return }
        if (!webPermissions.onRequestPermissionsResult(requestCode)) super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!browserStarted) {
            if (moe.pure.browser.feature.download.ui.DownloadPageIntents.matches(intent)) setIntent(intent)
            return
        }
        if (restoringSession) { queuedEntryIntent = Intent(intent); return }
        consumeIncomingIntent(intent)
    }

    private fun consumeIncomingIntent(intent: Intent) {
        if (!customTabMode) setIntent(intent)
        if (searchEntry(intent)) { closeAddressInput(); searchEntryConsumed = false; pendingSearchIntent = intent; dispatchSearchEntry(); return }
        if (moe.pure.browser.feature.download.ui.DownloadPageIntents.matches(intent)) { consumeDownloadEntry(intent); return }
        if (intent.action == ReadAloudController.ACTION_SHOW || intent.dataString == ReadAloudController.INTERNAL_URL) {
            readAloud.consume(intent); return
        }
        intent.dataString?.takeIf(TranslationController::isInternal)?.let { address ->
            intent.data = null; translation.consume(address); return
        }
        prepareEntry(intent) { entry -> entry?.let(::openEntry) }
    }

    private fun consumeDownloadEntry(value: Intent) {
        if (!browserReady() || !moe.pure.browser.feature.download.ui.DownloadPageIntents.matches(value) || supportFragmentManager.isStateSaved) return
        if (!pageStack.returnToBase()) return
        moe.pure.browser.feature.download.ui.DownloadPageIntents.consume(value)
        openDownloads(true)
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        if (settingsRecreationPending) { if (sessionDelegate.isInitialized()) session.save(); return }
        rootHost.resume()
        if (!browserStarted) {
            if (firstRunDelegate.isInitialized()) firstRun.resume()
            if (engineDelegate.isInitialized()) engine.resume()
            return
        }
        resumeBrowser()
    }

    private fun resumeBrowser() {
        if (!browserStarted || isDestroyed || isFinishing) return
        if (bookmarkChooserDelegate.isInitialized()) bookmarkChooser.rebind()
        if (BookmarkPopupController.hasRestored(this)) bookmarkPopup.rebind()
        if (scriptsDelegate.isInitialized()) scripts.bindRestored(currentTab())
        moe.pure.browser.feature.scripts.runtime.UserScriptEngine.get(this)
        if (!::chrome.isInitialized) return
        chrome.refreshIcons()
        chrome.refreshTypography()
        val desired = moe.pure.browser.ui.chrome.ChromeLayoutPolicy.resolve(this)
        if (desired != chrome.layoutSpec) {
            find.close()
            undoClose.close()
            switcher.close(animate = false)
            pageInfo.close()
            val previous = chrome
            createChrome()
            currentTab()?.let { attachTab(it, publishAddress = false) }
            previous.dispose()
        }
        applyWindowSettings()
        val reload = policyFingerprint?.let { it != webPolicy.reloadFingerprint() } == true
        applyWebSettings()
        if (reload) currentTab()?.takeIf { !it.home }?.reload()
        refreshChrome()
        if (downloadsDelegate.isInitialized()) downloads.onResume()
        if (shortcutDelegate.isInitialized()) shortcuts.onResume()
        search.refresh()
        home.refresh()
        tabs.asReversed().forEach { historyDocument.refreshIfDirty(it); generatedPages.refreshIfDirty(it) }
        reader.consumePageEffects()
        currentTab()?.let(appearance::refresh)
        if (passwordsDelegate.isInitialized()) passwords.resume()
        interactions.resume()
        syncing.resume()
        if (moe.pure.browser.feature.download.ui.DownloadPageIntents.matches(intent))
            chrome.root.post { consumeDownloadEntry(intent) }
    }

    override fun onPause() {
        activityResumed = false
        GeneratedPageDirtyState.persist(applicationContext)
        if (bookmarkChooserDelegate.isInitialized()) bookmarkChooser.paused()
        if (firstRunDelegate.isInitialized()) firstRun.pause()
        if (engineDelegate.isInitialized()) engine.pause()
        if (interactionsDelegate.isInitialized()) interactions.pause()
        super.onPause()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (interactionsDelegate.isInitialized()) interactions.pictureInPictureChanged(isInPictureInPictureMode)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("browser_started", browserStarted)
        if (firstRunDelegate.isInitialized()) firstRun.save(outState)
        if (engineDelegate.isInitialized()) engine.save(outState)
        if (!browserStarted) { super.onSaveInstanceState(outState); return }
        if (!featuresRestored) {
            listOf("downloads", "shortcut", "read_aloud", "bookmark_chooser").forEach { key ->
                pendingFeatureState?.getBundle(key)?.let { outState.putBundle(key, it) }
            }
        }
        outState.putBoolean("entry_pending", restoringSession)
        outState.putBoolean("search_entry_consumed", searchEntryConsumed)
        if (downloadsDelegate.isInitialized()) outState.putBundle("downloads", downloads.save())
        if (shortcutDelegate.isInitialized()) shortcuts.save()?.let { outState.putBundle("shortcut", it) }
        if (readAloudDelegate.isInitialized()) outState.putBundle("read_aloud", readAloud.save())
        if (bookmarkChooserDelegate.isInitialized()) bookmarkChooser.save()?.let { outState.putBundle("bookmark_chooser", it) }
        if (!privateMode) { outState.putBoolean("restore_session", true); session.save() }
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Framework retention keeps private state in memory across theme recreation")
    override fun onRetainCustomNonConfigurationInstance(): Any? = if (browserStarted && sessionDelegate.isInitialized()) session.retainInMemory() else null

    override fun onStop() {
        if (passwordsDelegate.isInitialized()) passwords.backgrounded()
        if (sessionDelegate.isInitialized()) session.save()
        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        val before = if (::chrome.isInitialized) chrome else null
        super.onConfigurationChanged(newConfig)
        if (!browserStarted) return
        // ThemedActivity already handles an actual night change. Rotation is geometry only.
        if (before === chrome) refreshChromeConfiguration()
        interactions.configurationChanged(newConfig)
        applyWindowSettings()
    }

    private fun refreshChromeConfiguration() {
        val desired = moe.pure.browser.ui.chrome.ChromeLayoutPolicy.resolve(this)
        if (desired != chrome.layoutSpec) {
            find.close(); undoClose.close(); switcher.close(animate = false); pageInfo.close()
            val previous = chrome
            val viewport = previous.captureViewportState()
            val progress = previous.captureProgressState()
            createChrome()
            currentTab()?.let { attachTab(it, publishAddress = false, refreshDocument = false) }
            chrome.restoreProgressState(progress)
            chrome.restoreViewportState(viewport)
            previous.dispose()
        } else {
            chrome.refreshViewportPolicy()
            refreshChrome()
        }
        home.refresh()
        if (searchDelegate.isInitialized()) search.rebind()
        if (menuDelegate.isInitialized()) menu.close()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (sessionDelegate.isInitialized()) session.trimMemory(level)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !initialWindowLayoutUpdated) {
            initialWindowLayoutUpdated = true
            val decor = window.decorView
            decor.post {
                if (decor.isAttachedToWindow && !isFinishing)
                    windowManager.updateViewLayout(decor, window.attributes)
            }
        }
        if (browserStarted && settings.fullscreen) NativeFullscreenState.apply(window, true)
        if (::chrome.isInitialized && gameActive) game.onWindowFocusChanged(hasFocus)
        if (interactionsDelegate.isInitialized()) interactions.windowFocusChanged(hasFocus)
    }

    override fun onDestroy() {
        if (startupVersionDelegate.isInitialized()) startupVersion.close()
        if (networkObserverDelegate.isInitialized()) networkObserverDelegate.value.close()
        browserGeneration++
        if (firstRunDelegate.isInitialized()) firstRun.dispose()
        if (engineDelegate.isInitialized()) engine.dispose()
        if (remoteSearchDelegate.isInitialized()) remoteSearch.close()
        if (databaseStartupDelegate.isInitialized()) databaseStartup.dispose()
        if (bookmarkChooserDelegate.isInitialized()) bookmarkChooser.dispose()
        if (closedTabsDelegate.isInitialized()) closedTabs.close()
        if (redirectsDelegate.isInitialized()) redirects.dispose()
        if (entriesDelegate.isInitialized()) entries.dispose()
        queuedEntryIntent = null
        if (customTabsDelegate.isInitialized()) customTabs.dispose()
        if (errorsDelegate.isInitialized()) errors.dispose()
        if (pageContextDelegate.isInitialized()) pageContext.dispose()
        if (imageModeDelegate.isInitialized()) imageMode.dispose()
        if (appearanceDelegate.isInitialized()) appearance.dispose()
        if (menuDelegate.isInitialized()) menu.close()
        if (interactionsDelegate.isInitialized()) interactions.dispose()
        tabStrip?.dispose(); tabStrip = null
        if (syncDelegate.isInitialized()) syncing.dispose()
        if (passwordsDelegate.isInitialized()) passwords.dispose()
        if (homeDelegate.isInitialized()) home.dispose()
        if (favoritesDelegate.isInitialized()) favorites.dispose()
        if (searchDelegate.isInitialized()) search.dispose()
        if (aiDelegate.isInitialized()) ai.dispose()
        if (reportDelegate.isInitialized()) reports.dispose()
        if (gameDelegate.isInitialized()) { game.dispose(isChangingConfigurations); gameMessages.close() }
        if (translationDelegate.isInitialized()) translation.dispose()
        if (homeSuggestionsDelegate.isInitialized()) homeSuggestions.close()
        if (readAloudDelegate.isInitialized()) readAloud.dispose(isChangingConfigurations)
        if (readerDelegate.isInitialized()) reader.dispose()
        if (pickerDelegate.isInitialized()) picker.dispose()
        if (downloadsDelegate.isInitialized()) downloads.dispose()
        if (shortcutDelegate.isInitialized()) shortcuts.dispose()
        if (scriptsDelegate.isInitialized()) scripts.dispose(isChangingConfigurations)
        if (toolsDelegate.isInitialized()) tools.dispose()
        if (pageFontDelegate.isInitialized()) pageFont.close()
        if (historyVisitsDelegate.isInitialized()) historyVisits.dispose()
        if (historyDocumentDelegate.isInitialized()) historyDocument.dispose()
        if (generatedPagesDelegate.isInitialized()) generatedPages.dispose()
        if (configuredStartDelegate.isInitialized()) configuredStart.dispose()
        if (restoreNoticeDelegate.isInitialized()) restoreNotice.dispose()
        if (bookmarkPopupDelegate.isInitialized()) bookmarkPopup.close()
        if (bookmarkDocumentActionsDelegate.isInitialized()) bookmarkDocumentActions.dispose()
        if (savedDocumentActionsDelegate.isInitialized()) savedDocumentActions.dispose()
        if (savePageDelegate.isInitialized()) savePage.close()
        if (::external.isInitialized) external.dispose()
        if (::pageInfo.isInitialized) pageInfo.close()
        if (qrCodeDelegate.isInitialized()) qrCode.dispose()
        if (::chrome.isInitialized) { chrome.root.animate().cancel(); chrome.dispose() }
        if (::undoClose.isInitialized) undoClose.close()
        if (::switcher.isInitialized) switcher.close(animate = false)
        if (webPermissionsDelegate.isInitialized()) webPermissions.dispose()
        if (sessionDelegate.isInitialized()) session.dispose()
        if (agentRepositoryDelegate.isInitialized()) agentRepository.close()
        if (libraryDelegate.isInitialized()) library.close()
        if (siteRepositoryDelegate.isInitialized()) siteRepository.close()
        if (storeDelegate.isInitialized()) databaseWorker.execute { store.close() }
        databaseWorker.shutdown()
        rootHost.close()
        super.onDestroy()
    }

}


