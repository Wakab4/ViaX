package moe.pure.browser.feature.bookmarks.document

import moe.pure.browser.feature.internalpages.render.GeneratedPageStyle

/** Window-resolved strings/style; the generator stores no Activity or live window. */
data class BookmarksDocumentOptions(val title: String, val empty: String, val sortOrder: Int, val style: GeneratedPageStyle)
