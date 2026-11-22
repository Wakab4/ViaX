package moe.via.browser.feature.startpage.document

import moe.via.browser.feature.library.bookmarks.document.BookmarksDocumentOptions

/** A configured command's raw choice and window strings; no Activity, View, or repository owner. */
data class ConfiguredStartPageRequest(
    val configured: String,
    val blankTitle: String,
    val bookmarkOptions: BookmarksDocumentOptions
)
