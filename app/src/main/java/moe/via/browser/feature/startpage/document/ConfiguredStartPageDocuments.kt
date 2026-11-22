package moe.via.browser.feature.startpage.document

import android.content.Context
import android.view.View
import moe.via.browser.R
import moe.via.browser.feature.library.bookmarks.document.BookmarksDocument
import moe.via.browser.feature.library.bookmarks.document.BookmarksDocumentOptions
import moe.via.browser.feature.home.HomeDocument
import moe.via.browser.feature.home.data.HomePreferences
import moe.via.browser.feature.internalpages.render.GeneratedPageInputs

/** ua.J/M0/N0: only configured start commands use this resolver, never explicit URL navigation. */
object ConfiguredStartPageDocuments {
    /** Capture command-time configuration and localized values without querying rows or writing files. */
    fun prepare(window: Context): ConfiguredStartPageRequest {
        val app = window.applicationContext
        val rtl = window.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        return ConfiguredStartPageRequest(
            configured = HomePreferences(app).homepage,
            blankTitle = window.getString(R.string.ref_blank_page_title),
            bookmarkOptions = BookmarksDocumentOptions(
                title = window.getString(R.string.ref_bookmark_page_title),
                empty = window.getString(R.string.ref_bookmark_page_empty),
                sortOrder = GeneratedPageInputs.bookmarkOrder(app),
                style = GeneratedPageInputs.style(app, rtl)
            )
        )
    }

    /** Synchronous worker generation; unknown, differently cased, and whitespace values stay raw. */
    fun generate(context: Context, request: ConfiguredStartPageRequest): String {
        val app = context.applicationContext
        return when (request.configured) {
            "about:home" -> HomeDocument.sourceUrl(app)
            "about:bookmarks" -> BookmarksDocument(app).generate(request.bookmarkOptions.copy(
                sortOrder = GeneratedPageInputs.bookmarkOrder(app),
                style = GeneratedPageInputs.style(app, request.bookmarkOptions.style.rtl)
            ))
            "about:blank" -> BlankDocument(app).generate(request.blankTitle)
            else -> request.configured
        }
    }
}
