package moe.pure.browser.feature.library.format

import moe.pure.browser.core.logging.NativeLog as Log
import moe.pure.browser.feature.library.data.BookmarkText
import moe.pure.browser.feature.library.data.NativeBookmarkFolder
import moe.pure.browser.feature.library.data.NativeBookmarkItem
import moe.pure.browser.feature.library.data.NativeBookmarkSnapshot
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.util.UUID

/** z8.p.q/r: HTML has no persistent IDs; every parsed record receives a fresh UUID. */
object NativeBookmarkHtml {
    private class Folder(var row: NativeBookmarkFolder)

    fun read(reader: Reader): NativeBookmarkSnapshot {
        val tokens = BookmarkHtmlTokens(reader)
        val folders = mutableListOf<Folder>()
        val items = mutableListOf<NativeBookmarkItem>()
        val parents = mutableListOf<Folder>()
        var item: NativeBookmarkItem? = null
        var lastText: String? = null
        var folderOrder = 0
        var itemOrder = 0
        while (true) {
            val type = try { tokens.next() } catch (error: IOException) {
                Log.warn("BookmarkHtml", "Read bookmark token", error)
                continue
            }
            if (type == BookmarkHtmlTokens.END) break
            val token = tokens.element
            when (type) {
                BookmarkHtmlTokens.TEXT -> if (item != null || parents.isNotEmpty()) lastText = BookmarkHtmlEntities.names(token)
                BookmarkHtmlTokens.CLOSE -> when {
                    token.equals("</DL>", true) -> {
                        if (parents.isNotEmpty()) parents.removeAt(parents.lastIndex)
                        itemOrder = 0
                    }
                    token.equals("</A>", true) -> item?.let {
                        items += it.copy(title = BookmarkText.clean(BookmarkHtmlEntities.decimals(lastText)))
                        item = null
                    }
                    token.equals("</H3>", true) && parents.isNotEmpty() -> {
                        val folder = parents.last()
                        folder.row = folder.row.copy(title = BookmarkText.clean(BookmarkHtmlEntities.decimals(lastText)))
                        folders += folder
                    }
                    else -> Unit
                }
                BookmarkHtmlTokens.OPEN -> when {
                    token.equals("<DL>", true) -> Unit
                    token.startsWith("<A ") || token.startsWith("<a ") -> {
                        val id = UUID.randomUUID().toString()
                        val url = BookmarkHtmlEntities.href(tokens.attribute("href"))?.trim { it <= ' ' }
                        item = if (invalidUrl(url)) null else {
                            val created = date(tokens, "add_date").takeUnless { it == 0L } ?: now()
                            NativeBookmarkItem(id, url, null, parents.lastOrNull()?.row?.id.orEmpty(), ++itemOrder,
                                created, date(tokens, "last_modified").takeUnless { it == 0L } ?: created)
                        }
                    }
                    token.startsWith("<H3") || token.startsWith("<h3") -> {
                        val id = UUID.randomUUID().toString()
                        val created = date(tokens, "add_date").takeUnless { it == 0L } ?: now()
                        parents += Folder(NativeBookmarkFolder(id, null, parents.lastOrNull()?.row?.id.orEmpty(),
                            ++folderOrder, created, date(tokens, "last_modified").takeUnless { it == 0L } ?: created))
                    }
                    else -> Unit
                }
                else -> Unit
            }
        }
        return NativeBookmarkSnapshot(folders.map { it.row }, items)
    }

    /** The returned count includes all items, even orphan items not reached from the root. */
    fun write(writer: Writer, snapshot: NativeBookmarkSnapshot): Int {
        if (snapshot.folders.isEmpty() && snapshot.items.isEmpty()) return 0
        var count = 0
        try {
            writer.use { output ->
                output.write(HEADER)
                val folders = snapshot.folders.sortedBy { it.order }
                val items = snapshot.items.sortedBy { it.order }
                fun folder(id: String, depth: Int) {
                    val indent = "  ".repeat(depth)
                    for (child in folders) if (child.parentId == id) {
                        output.write("$indent<DT><H3 ADD_DATE=\"${child.createdAtSeconds}\"" + modified(child.createdAtSeconds, child.updatedAtSeconds) +
                            ">${BookmarkHtmlEntities.title(child.title)}</H3>\n$indent<DL><p>\n")
                        folder(child.id, depth + 1)
                        output.write("$indent</DL><p>\n")
                    }
                    for (entry in items) if (entry.folderId == id) output.write(
                        "$indent<DT><A HREF=\"${BookmarkHtmlEntities.attribute(entry.url)}\" ADD_DATE=\"${entry.createdAtSeconds}\"" +
                            modified(entry.createdAtSeconds, entry.updatedAtSeconds) + ">${BookmarkHtmlEntities.title(entry.title)}</A>\n")
                }
                folder("", 1)
                output.write("</DL><p>\n")
                count = items.size
            }
        } catch (error: Exception) { Log.warn("BookmarkHtml", "Write bookmark HTML", error) }
        return count
    }

    private fun date(tokens: BookmarkHtmlTokens, name: String) = tokens.attribute(name)?.toLongOrNull() ?: 0L
    private fun modified(created: Long, updated: Long) = if (updated == created) "" else " LAST_MODIFIED=\"$updated\""
    private fun now() = System.currentTimeMillis() / 1000
    fun invalidUrl(url: String?) = url.isNullOrEmpty() || url.length > 1048576

    private const val HEADER = "<!DOCTYPE NETSCAPE-Bookmark-file-1>\n<!-- This is an automatically generated file.\n" +
        "     It will be read and overwritten.\n     DO NOT EDIT! -->\n<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n" +
        "<TITLE>Bookmarks</TITLE>\n<H1>Bookmarks</H1>\n<DL><p>\n"
}
