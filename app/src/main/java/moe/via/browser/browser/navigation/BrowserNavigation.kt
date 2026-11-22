package moe.via.browser.browser.navigation

import android.net.Uri
import android.webkit.WebView
import moe.via.browser.browser.session.BrowserTab
import moe.via.browser.feature.adblock.AdblockNavigationState
import moe.via.browser.feature.home.HomeDocument
import moe.via.browser.feature.home.HomeStartPageMatches
import moe.via.browser.feature.home.ui.favorites.HomeFavoritesController
import moe.via.browser.feature.internalpages.BrowserInternalPages
import moe.via.browser.feature.internalpages.GeneratedAddress
import moe.via.browser.feature.library.LibraryTab
import moe.via.browser.feature.library.pages.LibraryNavigationEntry
import moe.via.browser.feature.readaloud.ReadAloudController
import moe.via.browser.feature.search.SearchController
import moe.via.browser.feature.settings.SettingsEntry
import moe.via.browser.feature.settings.SettingsRoute
import moe.via.browser.feature.translation.TranslationController
import moe.via.browser.ui.theme.ThemedActivity

internal class BrowserNavigation(private val activity: ThemedActivity, private val host: Host) {
    interface Host {
        fun currentTab(): BrowserTab?
        fun newTab(url: String)
        fun refreshChrome()
        fun preparePage(tab: BrowserTab, source: WebView, url: String)
        fun consumeErrorLink(tab: BrowserTab, source: WebView, url: String): Boolean
        fun openExternal(url: String, origin: String): Boolean
        fun postWhenReady(action: () -> Unit)
        fun openSearch(url: String)
        fun showReadAloud()
        fun translate(url: String)
        fun showHistoryDocument()
        fun showGeneratedPage(kind: Int, argument: String? = null)
        fun findBookmarkFolder(title: String): String
        fun showBookmarkChooser(folder: String?)
        fun showScanner()
        fun showDownloads()
        fun restoreClosedTabs(url: String)
        fun showHome()
    }

    fun load(tab: BrowserTab, url: String) {
        tab.navigate(HomeDocument.normalize(activity, url))
        if (tab === host.currentTab()) host.refreshChrome()
    }

    fun loadFrom(tab: BrowserTab, source: WebView, url: String) {
        if (tab.isCurrent(source)) load(tab, url)
        else if (tab.owns(source)) {
            val target = HomeDocument.normalize(activity, url)
            host.preparePage(tab, source, target)
            tab.urlChanged(source, target)
            source.loadUrl(target)
        }
    }

    fun fromPage(tab: BrowserTab, source: WebView, url: String): Boolean {
        val homeTarget = HomeDocument.normalize(activity, url)
        if (homeTarget != url || HomeDocument.isHome(activity, url) && !HomeDocument.isHome(activity, tab.sourceUrl(source))) {
            loadFrom(tab, source, homeTarget)
            return true
        }
        if (host.consumeErrorLink(tab, source, url)) return true
        val favoriteTarget = HomeFavoritesController.unwrap(url)
        if (favoriteTarget != url) { fromInput(favoriteTarget); return true }
        if (consumePageAction(url)) return true
        if (url.startsWith("history://", ignoreCase = true) || url.startsWith("folder://", ignoreCase = true) ||
            GeneratedAddress.isV(GeneratedAddress.normalize(url))
        ) {
            fromInput(url)
            return true
        }
        val uri = runCatching { Uri.parse(url) }.getOrNull()
        if (uri?.scheme in listOf("v", "via") && uri?.host in setOf("bookmarks", "history", "tabs", "downloader", "scanner")) {
            fromInput(url)
            return true
        }
        if (uri?.scheme in setOf("folder", "history")) { fromInput(url); return true }
        if (uri?.scheme in setOf("v", "via")) return true
        return host.openExternal(url, tab.sourceUrl(source).orEmpty())
    }

    fun fromInput(input: String, createTab: Boolean = false) {
        val url = GeneratedAddress.normalize(input)
        val owner = host.currentTab()
        val view = owner?.liveView
        if (owner != null && view != null && host.consumeErrorLink(owner, view, url)) return
        if (url.startsWith("v://blocker/jump?url=", ignoreCase = true)) {
            val target = AdblockNavigationState.target(Uri.parse(url))
            if (target != null) {
                owner?.let { AdblockNavigationState.of(it).allow(target) }
                host.postWhenReady { host.currentTab()?.let { load(it, target) } }
            }
            return
        }
        if (consumePageAction(url)) return
        if (url.startsWith("javascript:", true)) { host.currentTab()?.liveView?.loadUrl(url); return }
        if (url.startsWith("history://", ignoreCase = true)) { host.showHistoryDocument(); return }
        if (url.startsWith("folder://", ignoreCase = true)) { openFolder(url.substring(9)); return }
        val homeTarget = HomeDocument.normalize(activity, url)
        if (homeTarget != url) { openTabOrLoad(homeTarget, createTab); return }
        if (GeneratedAddress.isV(url)) { openInternal(url); return }
        val current = host.currentTab()
        if (host.openExternal(url, current?.url.orEmpty())) return
        if (createTab || current == null) host.newTab(url) else load(current, url)
    }

    fun showLibrary(history: Boolean) {
        LibraryNavigationEntry.open(activity, if (history) LibraryTab.HISTORY else LibraryTab.BOOKMARKS)
    }

    private fun openTabOrLoad(url: String, createTab: Boolean) {
        val current = host.currentTab()
        if (createTab || current == null) host.newTab(url) else load(current, url)
    }

    private fun consumePageAction(url: String): Boolean {
        when {
            SearchController.isSearchUrl(url) -> host.openSearch(url)
            url == ReadAloudController.INTERNAL_URL -> host.showReadAloud()
            TranslationController.isInternal(url) -> host.translate(url)
            else -> return false
        }
        return true
    }

    private fun openFolder(argument: String) {
        if (currentInternalKind() in setOf(2, 11)) {
            host.showGeneratedPage(if (argument.isEmpty()) 2 else 11, argument)
        } else {
            val folderId = host.findBookmarkFolder(Uri.decode(argument))
            LibraryNavigationEntry.open(activity, LibraryTab.BOOKMARKS, folderId = folderId)
        }
    }

    private fun openInternal(url: String) {
        when {
            GeneratedAddress.matches(url, "v://bookmarks") -> openBookmarks(GeneratedAddress.folder(url))
            GeneratedAddress.matches(url, "v://history") -> if (currentInternalKind() == 5) host.showHistoryDocument() else showLibrary(true)
            GeneratedAddress.matches(url, "v://scanner") -> host.showScanner()
            GeneratedAddress.matches(url, "v://downloader") -> host.showDownloads()
            GeneratedAddress.matches(url, "v://readaloud") -> host.showReadAloud()
            GeneratedAddress.matches(url, "v://search") -> host.openSearch("v://search" + url.substring(10))
            GeneratedAddress.matches(url, "v://skins") -> SettingsEntry.open(activity, SettingsRoute.SKINS)
            url.startsWith("v://tabs", ignoreCase = true) -> host.restoreClosedTabs(url)
            else -> when (val kind = GeneratedAddress.kind(url)) {
                1 -> host.currentTab()?.takeUnless { HomeStartPageMatches.matches(activity, it.liveView?.url ?: it.url) }
                    ?.let { host.showHome() }
                3 -> host.showHistoryDocument()
                else -> host.showGeneratedPage(kind)
            }
        }
    }

    private fun openBookmarks(folder: String?) {
        val requested = folder?.takeIf { it.isNotEmpty() }
        when (currentInternalKind()) {
            2, 5, 11 -> host.showGeneratedPage(if (requested == null) 2 else 11, requested)
            1 -> host.showBookmarkChooser(requested)
            else -> LibraryNavigationEntry.open(activity, LibraryTab.BOOKMARKS, folderId = requested.orEmpty())
        }
    }

    private fun currentInternalKind(): Int {
        val tab = host.currentTab()
        return BrowserInternalPages.kind(activity, tab?.liveView?.url ?: tab?.url)
    }
}
