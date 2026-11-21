package moe.pure.browser.feature.library

import moe.pure.browser.feature.internalpages.GeneratedPageDirtyState
import moe.pure.browser.feature.library.format.NativeBookmarkHtml
import java.io.Reader
import java.io.Writer

/** Manual HTML and archive HTML share z8.p's format and the same TEXT-identity import path. */
object BookmarkHtmlCodec {
    @Suppress("UNUSED_PARAMETER")
    fun import(reader: Reader, repository: LibraryRepository, destination: String = ""): Int {
        // Native's manual picker is a root-level merge; it does not relocate the parsed graph.
        val count = repository.importNativeBookmarks(NativeBookmarkHtml.read(reader))
        GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.BOOKMARKS)
        return count
    }

    @Suppress("UNUSED_PARAMETER")
    fun export(writer: Writer, repository: LibraryRepository, root: String = ""): Int =
        NativeBookmarkHtml.write(writer, repository.nativeBookmarks())
}
