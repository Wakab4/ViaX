package moe.pure.browser.feature.startpage.document

import moe.pure.browser.feature.bookmarks.document.BookmarksDocumentOptions

/** A configured command's raw choice and window strings; no Activity, View, or repository owner. */
data class ConfiguredStartPageRequest(
    val configured: String,
    val blankTitle: String,
    val bookmarkOptions: BookmarksDocumentOptions
)
