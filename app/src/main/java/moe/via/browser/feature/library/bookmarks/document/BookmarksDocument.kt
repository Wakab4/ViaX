package moe.via.browser.feature.library.bookmarks.document

import android.content.Context
import moe.via.browser.core.logging.NativeLog as Log
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import moe.via.browser.feature.internalpages.GeneratedPageFiles
import moe.via.browser.feature.internalpages.render.GeneratedListCss
import moe.via.browser.feature.internalpages.render.GeneratedPageMarkup
import moe.via.browser.feature.library.BookmarkSort
import moe.via.browser.feature.library.LibraryEntry
import moe.via.browser.feature.library.LibraryKind
import moe.via.browser.feature.library.data.LibraryRepository
import moe.via.browser.feature.library.data.BookmarkOrdering
import java.io.BufferedWriter
import java.io.FileWriter

/** u9.c.d: root is dirty8 cached; an explicit folder always overwrites the shared folder.html. */
class BookmarksDocument(context: Context) {
    private val app = context.applicationContext
    private val ordering by lazy { BookmarkOrdering(app) }

    fun generate(options: BookmarksDocumentOptions, folder: String? = null): String {
        val rootRequest = folder.isNullOrEmpty()
        val file = GeneratedPageFiles.file(app, if (rootRequest) ROOT else FOLDER)
        var resolved = folder
        if (!rootRequest || GeneratedPageDirtyState.isDirty(GeneratedPageDirtyState.BOOKMARKS) || !file.exists()) {
            try {
                FileWriter(file).use { output -> BufferedWriter(output).use { writer ->
                    LibraryRepository(app).use { repository ->
                        val stored = folder?.let(repository::findFolder)?.takeUnless { it.id.isEmpty() } ?: root()
                        val current = stored.copy(title = BookmarkDocumentTitle.clean(stored.title))
                        resolved = BookmarkDocumentLinks.key(current.id)
                        val rows = ordering.sort(repository.bookmarks(current.id, sort = BookmarkSort.OLDEST)
                            .map { it.copy(title = BookmarkDocumentTitle.clean(it.title)) }, options.sortOrder).toMutableList()
                        if (!resolved.isNullOrEmpty()) {
                            val parent = repository.findFolder(current.parentId) ?: root()
                            rows.add(0, parent.copy(title = ".."))
                        }
                        writer.write(GeneratedPageMarkup.head(if (rootRequest || current.title.isEmpty()) options.title else current.title))
                        writer.write("<style>${GeneratedListCss.render(options.style)}</style>")
                        writer.write("<body><div class='frosted-glass' id='gesture-indicator'></div><div id=\"content\">")
                        writeRows(writer, rows)
                        if (rows.isEmpty() && rootRequest) writer.write(GeneratedPageMarkup.hint(options.empty))
                        writer.write("</div></body></html>")
                    }
                } }
                // Native clears root dirty only after both writers have closed successfully.
                if (rootRequest) GeneratedPageDirtyState.clear(GeneratedPageDirtyState.BOOKMARKS)
            } catch (error: Exception) { Log.warn("BookmarkDocument", "Generate bookmarks", error) }
        }
        if (!rootRequest && resolved.isNullOrEmpty() && GeneratedPageDirtyState.isDirty(GeneratedPageDirtyState.BOOKMARKS)) {
            return generate(options)
        }
        return url(app, resolved)
    }

    private fun writeRows(writer: BufferedWriter, rows: List<LibraryEntry>) {
        var folders = false
        var bookmarks = false
        for (entry in rows) {
            if (!entry.isFolder) {
                if (folders && !bookmarks) { writer.write("</div>"); writer.write("<div id=\"bookmark_tags\">") }
                else if (!bookmarks) writer.write("<div id=\"bookmark_tags\">")
                bookmarks = true
            } else if (!folders) { writer.write("<div id=\"bookmark_folders\">"); folders = true }
            val address = if (entry.isFolder) BookmarkDocumentLinks.folder(BookmarkDocumentLinks.key(entry.id))
                else BookmarkDocumentLinks.bookmark(entry.url)
            writer.write(GeneratedPageMarkup.row(address, entry.title, if (entry.isFolder) "tag" else "bookmark"))
        }
        // u9.c.d does not add this closing wrapper for an all-folder result.
        if (bookmarks) writer.write("</div>")
    }
    private fun root() = LibraryEntry("", LibraryKind.FOLDER, "")
    companion object {
        private const val ROOT = "bookmarks.html"
        private const val FOLDER = "folder.html"
        fun url(context: Context, folder: String? = null): String = GeneratedPageFiles.url(context, if (folder.isNullOrEmpty()) ROOT else FOLDER) +
            if (folder.isNullOrEmpty()) "" else "?folder=$folder"
        fun isPage(context: Context, url: String?) = GeneratedPageFiles.isPage(context, url, ROOT) || GeneratedPageFiles.isPage(context, url, FOLDER)
        /** ua.x1 takes the last literal marker's entire suffix and Java-trims it, retaining any &/#. */
        fun folderArgument(url: String): String? = url.lastIndexOf("?folder=").takeIf { it > 0 }
            ?.let { url.substring(it + 8).trim { char -> char <= ' ' } }
    }
}
