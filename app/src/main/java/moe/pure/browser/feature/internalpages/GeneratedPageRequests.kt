package moe.pure.browser.feature.internalpages

import android.content.Context
import android.view.View
import moe.pure.browser.R
import moe.pure.browser.feature.about.document.AboutDocument
import moe.pure.browser.feature.about.document.AboutDocumentOptions
import moe.pure.browser.feature.about.document.AboutDocumentTexts
import moe.pure.browser.feature.bookmarks.document.BookmarksDocument
import moe.pure.browser.feature.bookmarks.document.BookmarksDocumentOptions
import moe.pure.browser.feature.catalog.document.CatalogDocument
import moe.pure.browser.feature.catalog.document.CatalogDocumentOptions
import moe.pure.browser.feature.firstrun.FirstRunDocument
import moe.pure.browser.feature.home.data.HomePreferences
import moe.pure.browser.feature.internalpages.render.GeneratedPageStyle
import moe.pure.browser.feature.internalpages.render.GeneratedPageInputs
import moe.pure.browser.feature.network.NetworkLogPages
import moe.pure.browser.feature.network.NetworkLogDocumentOptions
import moe.pure.browser.feature.network.NetworkLogDocumentTexts
import moe.pure.browser.feature.adblock.rules.CustomRuleFiles
import moe.pure.browser.feature.offline.document.SavedDocument
import moe.pure.browser.feature.offline.document.SavedDocumentOptions
import moe.pure.browser.ui.theme.BrowserNightMode
import moe.pure.browser.feature.history.document.HistoryDocument
import moe.pure.browser.feature.history.document.HistoryDocumentOptions
import moe.pure.browser.feature.startpage.document.BlankDocument
import moe.pure.browser.feature.home.HomeDocument
import moe.pure.browser.feature.settings.data.BrowserPreferences

/** Resolve window text/style on Main; prepared jobs contain only immutable rendering inputs. */
internal object GeneratedPageRequests {
    fun prepare(context: Context, kind: Int, argument: String?, network: GeneratedNetworkSource?): GeneratedPageRequest? {
        val preferences = context.getSharedPreferences("browser", Context.MODE_PRIVATE)
        val config = HomePreferences(context).config(night = BrowserNightMode.global(context),
            isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL)
        val style = GeneratedPageStyle(config.isRtl, config.isNight, config.isLight)
        return when (kind) {
            1 -> HomepageRequest
            2, 11 -> BookmarkRequest(BookmarksDocumentOptions(context.getString(R.string.ref_bookmark_page_title),
                context.getString(R.string.ref_bookmark_page_empty), GeneratedPageInputs.bookmarkOrder(context), style), argument)
            3 -> HistoryRequest(HistoryDocumentOptions(context.getString(R.string.ref_history_page_title),
                context.getString(R.string.ref_history_page_empty), context.getString(R.string.ref_history_page_incognito),
                style.rtl, style.night, style.light, BrowserPreferences(preferences).incognito))
            4 -> AboutRequest(AboutDocumentTexts.options(context, style, FirstRunDocument.TERMS.url(preferences),
                FirstRunDocument.PRIVACY.url(preferences)))
            5 -> CatalogRequest(CatalogDocumentOptions(context.getString(R.string.ref_catalog_page_title), listOf(
                context.getString(R.string.ref_catalog_page_bookmarks), context.getString(R.string.ref_catalog_page_history),
                context.getString(R.string.ref_catalog_page_log), context.getString(R.string.ref_catalog_page_offline),
                context.getString(R.string.ref_catalog_page_home), context.getString(R.string.ref_catalog_page_about)), style))
            6, 10 -> NetworkRequest(network ?: GeneratedNetworkSource("", emptyList()), kind == 10,
                NetworkLogDocumentTexts.options(context, kind == 10, style, CustomRuleFiles.file(context).absolutePath))
            7 -> SavedRequest(SavedDocumentOptions(context.getString(R.string.ref_saved_page_title),
                context.getString(R.string.ref_saved_page_empty), context.getString(R.string.ref_saved_page_hint), style))
            12 -> BlankRequest(context.getString(R.string.ref_blank_page_title))
            else -> null
        }
    }
}

private data object HomepageRequest : GeneratedPageRequest {
    override fun generate(context: Context) = HomeDocument.sourceUrl(context)
}
private data class BlankRequest(val title: String) : GeneratedPageRequest {
    override fun generate(context: Context) = BlankDocument(context).generate(title)
}
private data class HistoryRequest(val options: HistoryDocumentOptions) : GeneratedPageRequest {
    override fun generate(context: Context): String {
        val style = GeneratedPageInputs.style(context, options.isRtl)
        return HistoryDocument(context).generate(options.copy(isNight = style.night, isLight = style.light,
            globalIncognito = BrowserPreferences(context.getSharedPreferences("browser", Context.MODE_PRIVATE)).incognito))
    }
}

internal sealed interface GeneratedPageRequest {
    fun generate(context: Context): String
}

private data class BookmarkRequest(val options: BookmarksDocumentOptions, val folder: String?) : GeneratedPageRequest {
    override fun generate(context: Context) = BookmarksDocument(context).generate(options.copy(
        style = GeneratedPageInputs.style(context, options.style.rtl), sortOrder = GeneratedPageInputs.bookmarkOrder(context)), folder)
}
private data class CatalogRequest(val options: CatalogDocumentOptions) : GeneratedPageRequest {
    override fun generate(context: Context) = CatalogDocument(context).generate(options.copy(style = GeneratedPageInputs.style(context, options.style.rtl)))
}
private data class AboutRequest(val options: AboutDocumentOptions) : GeneratedPageRequest {
    override fun generate(context: Context) = AboutDocument(context).generate(options.copy(style = GeneratedPageInputs.style(context, options.style.rtl)))
}
private data class SavedRequest(val options: SavedDocumentOptions) : GeneratedPageRequest {
    override fun generate(context: Context) = SavedDocument(context).generate(options.copy(style = GeneratedPageInputs.style(context, options.style.rtl)))
}
private data class NetworkRequest(val source: GeneratedNetworkSource, val resources: Boolean,
    val options: NetworkLogDocumentOptions) : GeneratedPageRequest {
    override fun generate(context: Context) = NetworkLogPages.open(context, source.tabId, source.requests, resources,
        options.copy(style = GeneratedPageInputs.style(context, options.style.rtl)))
}
