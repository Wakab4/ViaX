package moe.via.browser.feature.library.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import moe.via.browser.storage.database.BrowserDatabase
import moe.via.browser.feature.home.data.HomeFavoritesRepository
import moe.via.browser.feature.home.data.HomePreferences
import moe.via.browser.feature.home.data.HomeFavoriteUrls
import moe.via.browser.feature.internalpages.GeneratedPageDirtyState
import java.io.File
import java.util.Calendar
import moe.via.browser.feature.library.history.data.HistoryRecord
import moe.via.browser.feature.library.history.data.HistoryRows
import moe.via.browser.feature.library.history.HistoryRead
import moe.via.browser.feature.library.history.HistoryPeriod
import moe.via.browser.feature.library.history.HistoryQuery
import moe.via.browser.feature.offline.document.SavedArchiveFiles
import moe.via.browser.feature.library.BookmarkSort
import moe.via.browser.feature.library.LibraryEntry
import moe.via.browser.feature.library.LibraryKind

class LibraryRepository(context: Context, private val store: BrowserDatabase = BrowserDatabase(context)) : AutoCloseable {
    private val app = context.applicationContext
    private val folderVisibility = BookmarkFolderVisibility(app)
    private val db get() = store.writableDatabase
    private val homeFavorites = HomeFavoritesRepository(context, store, closeStore = false)
    private val homePreferences = HomePreferences(context)
    private val ordering = moe.via.browser.feature.library.data.BookmarkOrdering(context.applicationContext)
    private val historyRows get() = HistoryRows(db)

    private val nativeRows get() = moe.via.browser.feature.library.data.NativeBookmarkRows(db)

    fun bookmarks(folder: String = "", query: String = "", sort: BookmarkSort = BookmarkSort.OLDEST,
        includeFolderCounts: Boolean = false, showHidden: Boolean = false): List<LibraryEntry> {
        val rows = nativeRows
        val excluded = if (showHidden) emptySet() else folderVisibility.excludedIds(rows)
        if (folder in excluded) return emptyList()
        val hidden = if (showHidden) folderVisibility.explicitIds() else emptySet()
        val folders = (if (query.isBlank()) rows.children(folder)
            else rows.folders().filter { containsAsciiNoCase(it.title.orEmpty(), query) }).filterNot { it.id in excluded }
        val items = (if (query.isBlank()) rows.items(folder)
            else rows.items().filter { containsAsciiNoCase(it.title.orEmpty(), query) || containsAsciiNoCase(it.url.orEmpty(), query) })
            .filterNot { it.folderId in excluded }
        val counts = if (includeFolderCounts) folderChildCounts(excluded) else emptyMap()
        val values = folders.map { it.entry(hidden = it.id in hidden).copy(childCount = counts[it.id] ?: 0) } + items.map { it.entry() }
        return ordering.sort(values, sort.ordinal)
    }

    private fun folderChildCounts(excluded: Set<String>): Map<String, Int> = db.rawQuery("""
        SELECT folder_id,SUM(total) FROM (
            SELECT folder_id,COUNT(*) AS total FROM bookmark_items GROUP BY folder_id
            UNION ALL
            SELECT parent_folder_id AS folder_id,COUNT(*) AS total FROM bookmark_folders GROUP BY parent_folder_id
        ) GROUP BY folder_id
    """.trimIndent(), null).use { cursor ->
        buildMap {
            while (cursor.moveToNext()) put(cursor.getString(0).orEmpty(), cursor.getInt(1))
            if (excluded.isNotEmpty()) for (folder in nativeRows.folders()) {
                if (folder.id in excluded) computeIfPresent(folder.parentId) { _, count -> count - 1 }
            }
        }
    }

    fun findFolder(id: String, showHidden: Boolean = false): LibraryEntry? {
        val rows = nativeRows
        if (!showHidden && id in folderVisibility.excludedIds(rows)) return null
        return rows.folder(id)?.entry(hidden = showHidden && id in folderVisibility.explicitIds())
    }

    fun hasHiddenFolders(): Boolean = folderVisibility.explicitIds().isNotEmpty()

    fun setBookmarkFolderHidden(id: String, hidden: Boolean) {
        if (id.isEmpty() || nativeRows.folder(id) == null) return
        if (folderVisibility.setHidden(id, hidden)) {
            markBookmarksChanged()
            homePreferences.markDirty(html = true, style = false)
        }
    }

    fun isBookmarked(url: String): Boolean = store.readableDatabase.rawQuery(
        "SELECT 1 FROM bookmark_items WHERE url = ? LIMIT 1", arrayOf(url)
    ).use { it.moveToFirst() }

    fun folders(showHidden: Boolean = false): List<LibraryEntry> {
        val rows = nativeRows
        val excluded = if (showHidden) emptySet() else folderVisibility.excludedIds(rows)
        val hidden = if (showHidden) folderVisibility.explicitIds() else emptySet()
        return listOf(LibraryEntry("", LibraryKind.FOLDER, "")) +
            rows.folders().filterNot { it.id in excluded }.map { it.entry(hidden = it.id in hidden) }
    }
    fun findBookmark(url: String): LibraryEntry? = nativeRows.itemByUrl(listOf(url))?.entry()
    fun findBookmarkById(id: String): LibraryEntry? = nativeRows.item(id)?.entry()
    fun bookmarkUrls(folder: String, showHidden: Boolean = false): List<String> {
        val rows = nativeRows
        return if (!showHidden && folder in folderVisibility.excludedIds(rows)) emptyList()
        else rows.items(folder).mapNotNull { it.url }
    }
    fun findNativeBookmark(url: String): LibraryEntry? = nativeRows.itemByUrl(HomeFavoriteUrls.equivalents(url))?.entry()
    fun findFolderByTitle(title: String): LibraryEntry? {
        val rows = nativeRows
        val found = rows.folderByTitle(title) ?: return null
        val excluded = folderVisibility.excludedIds(rows)
        return if (found.id !in excluded) found.entry()
        else rows.folders().firstOrNull { it.id !in excluded && title.equals(it.title, ignoreCase = true) }?.entry()
    }
    fun searchFolders(raw: String): List<LibraryEntry> {
        val rows = nativeRows
        val excluded = folderVisibility.excludedIds(rows)
        return rows.searchFolders(raw).filterNot { it.id in excluded }.map { it.entry() }
    }
    fun nativeBookmarks(): moe.via.browser.feature.library.data.NativeBookmarkSnapshot = nativeRows.readAll()
    fun importNativeBookmarks(snapshot: moe.via.browser.feature.library.data.NativeBookmarkSnapshot): Int = nativeRows.importBatch(snapshot)

    fun saveBookmark(title: String, url: String, folder: String, id: String? = null,
        nowSeconds: Long = System.currentTimeMillis() / 1000): String {
        val saved = if (id == null) nativeRows.addItem(title, url, folder, nowSeconds)
            else nativeRows.updateItem(id, title, url, folder, nowSeconds)
        if (saved.isNotEmpty()) markBookmarksChanged()
        return saved
    }

    fun saveFolder(title: String, parent: String, id: String? = null,
        nowSeconds: Long = System.currentTimeMillis() / 1000, normalizeTitle: Boolean = true): String {
        if (id != null && !canMoveFolder(id, parent)) return ""
        val saved = if (id == null) nativeRows.addFolder(title, parent, nowSeconds, normalizeTitle)
            else nativeRows.updateFolder(id, title, parent, nowSeconds)
        if (saved.isNotEmpty()) markBookmarksChanged()
        return saved
    }

    /** b8.y keeps its loaded model; DAO success is not followed by a second lookup. */
    fun saveBookmarkEntry(original: LibraryEntry, title: String, url: String, folder: String,
        nowSeconds: Long = System.currentTimeMillis() / 1000): LibraryEntry? {
        val rows = nativeRows
        val saved = if (original.id.isEmpty()) rows.addItemRecord(title, url, folder, nowSeconds)
        else moe.via.browser.feature.library.data.NativeBookmarkItem(original.id, url,
            moe.via.browser.feature.library.data.BookmarkText.clean(title), folder, original.position.toInt(),
            original.createdAt, nowSeconds).takeIf(rows::updateItemRecord)
        if (saved != null) markBookmarksChanged()
        return saved?.entry()
    }

    fun createFolderEntry(title: String, parent: String, nowSeconds: Long = System.currentTimeMillis() / 1000,
        normalizeTitle: Boolean = true): LibraryEntry? {
        val saved = nativeRows.addFolderRecord(title, parent, nowSeconds, normalizeTitle)
        if (saved != null) markBookmarksChanged()
        return saved?.entry()
    }

    fun deleteBookmarkUrl(url: String): Int = nativeRows.deleteUrl(url).also { if (it > 0) markBookmarksChanged() }
    fun deleteBookmarkById(id: String): Boolean = (nativeRows.deleteItems(listOf(id)) > 0).also { if (it) markBookmarksChanged() }

    fun history(query: String = ""): List<LibraryEntry> = readHistory(
        if (query.isBlank()) null else "title LIKE ? ESCAPE '\\' OR url LIKE ? ESCAPE '\\'",
        if (query.isBlank()) null else arrayOf(like(query), like(query)), null).rows

    /** Dedicated Native paging API; existing history() consumers retain their original full-query semantics. */
    fun historyPage(offset: Int, limit: Int): HistoryRead = readHistory(null, null, "$offset,$limit")

    fun historySearch(raw: String, limit: Int = 1024): HistoryRead {
        val selection = HistoryQuery.selection(raw) ?: return HistoryRead(emptyList())
        return readHistory(selection.sql, selection.arguments, limit.takeIf { it > 0 }?.toString())
    }

    private fun readHistory(selection: String?, arguments: Array<String>?, limit: String?): HistoryRead {
        val result = historyRows.read(selection, arguments, limit)
        return HistoryRead(result.rows.map { it.libraryEntry() }, result.failure)
    }

    /** ga.b.i + bb.v.t use raw timezone offset and five cumulative time cutoffs, rather than loaded rows. */
    fun historyPeriodCounts(): List<HistoryPeriod> {
        val rawOffset = Calendar.getInstance().timeZone.rawOffset / 1000
        val counts = historyRows.periodCounts()
        val today = ((System.currentTimeMillis() / 1000 + rawOffset) / 86400) * 86400 - rawOffset
        val cutoffs = longArrayOf(counts[0].sinceSeconds, today, today - 86400, today - 518400, 0)
        val totals = IntArray(5); totals[0] = counts[0].count
        for (entry in counts.drop(1)) for (index in 1..4) if (entry.sinceSeconds >= cutoffs[index]) totals[index] += entry.count
        return cutoffs.mapIndexed { index, seconds -> HistoryPeriod(index, seconds, totals[index]) }
    }

    fun clearHistory(sinceSeconds: Long): Int = historyRows.clearSince(sinceSeconds)
    fun deleteHistory(ids: LongArray): Int = historyRows.deleteIds(ids)

    fun offline(query: String = ""): List<LibraryEntry> = SavedArchiveFiles.read(app).map { archive ->
        LibraryEntry("", LibraryKind.OFFLINE, archive.title, archive.url,
            createdAt = archive.file.lastModified(), path = archive.file.absolutePath, size = archive.file.length())
    }.let { rows -> if (query.isBlank()) rows else rows.filter { containsAsciiNoCase(it.title, query) || containsAsciiNoCase(it.url, query) } }

    fun favorites(): List<LibraryEntry> = homeFavorites.all().map {
        LibraryEntry(it.id.toString(), LibraryKind.FAVORITE, it.title, it.url, position = it.position.toLong(), createdAt = it.createdAt, updatedAt = it.updatedAt)
    }

    fun saveFavorite(title: String, url: String) {
        if (homeFavorites.save(title, url) > 0) GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.HOME)
    }

    fun delete(entries: List<LibraryEntry>) {
        var bookmarkChange = false
        val deletedFolders = mutableSetOf<String>()
        transaction {
            val historyIds = entries.filter { it.kind == LibraryKind.HISTORY }.map { it.numericId }.toLongArray()
            if (historyIds.isNotEmpty()) deleteHistory(historyIds)
            entries.forEach { entry ->
                if (entry.kind == LibraryKind.OFFLINE) {
                    val file = File(entry.path)
                    val url = "file://${file.parent}/${android.net.Uri.encode(file.name)}"
                    SavedArchiveFiles.delete(app, url)
                } else if (entry.isFolder && entry.id.isNotEmpty()) {
                    deletedFolders.addAll(nativeRows.deleteFolders(listOf(entry.id)))
                    bookmarkChange = true
                } else if (entry.kind == LibraryKind.BOOKMARK) {
                    nativeRows.deleteItems(listOf(entry.id)); bookmarkChange = true
                } else if (entry.kind != LibraryKind.HISTORY && !entry.isFolder) {
                    db.delete(table(entry), if (entry.kind == LibraryKind.FAVORITE) "f_id=?" else "_id=?", arrayOf(entry.id))
                }
            }
        }
        if (deletedFolders.isNotEmpty() && folderVisibility.forget(deletedFolders)) {
            homePreferences.markDirty(html = true, style = false)
        }
        if (bookmarkChange) markBookmarksChanged()
        if (entries.any { it.kind == LibraryKind.FAVORITE }) homePreferences.markDirty(html = true, style = false)
    }

    fun move(entries: List<LibraryEntry>, destination: String) {
        var changed = false
        transaction {
            require(entries.none { it.isFolder && !canMoveFolder(it.id, destination) })
            val now = System.currentTimeMillis() / 1000
            entries.forEach { entry ->
                require(entry.kind == LibraryKind.BOOKMARK || entry.isFolder)
                if (entry.isFolder) nativeRows.moveFolder(entry.id, destination, nativeRows.nextFolderOrder(), now)
                else nativeRows.moveItem(entry.id, destination, nativeRows.nextItemOrder(destination), now)
                changed = true
            }
        }
        if (changed) markBookmarksChanged()
    }

    fun reorder(entries: List<LibraryEntry>) {
        var bookmarkChange = false
        transaction {
            entries.forEachIndexed { index, entry ->
                when {
                    entry.isFolder -> { nativeRows.reorderFolder(entry.id, index); bookmarkChange = true }
                    entry.kind == LibraryKind.BOOKMARK -> { nativeRows.reorderItem(entry.id, index); bookmarkChange = true }
                    entry.kind == LibraryKind.FAVORITE -> db.update("favorites", ContentValues().apply { put("f_order", index) }, "f_id=?", arrayOf(entry.id))
                    else -> db.update(table(entry), ContentValues().apply { put("position", index.toLong()) }, "_id=?", arrayOf(entry.id))
                }
            }
        }
        if (bookmarkChange) markBookmarksChanged()
        if (entries.any { it.kind == LibraryKind.FAVORITE }) homePreferences.markDirty(html = true, style = false)
    }

    fun canMoveFolder(id: String, destination: String): Boolean {
        if (id.isEmpty() || id == destination) return false
        val parents = nativeRows.folders().associate { it.id to it.parentId }
        val seen = mutableSetOf<String>()
        var current = destination
        while (current.isNotEmpty() && seen.add(current)) {
            if (current == id) return false
            current = parents[current] ?: return false
        }
        return current.isEmpty()
    }

    fun <T> transaction(action: () -> T): T {
        db.beginTransaction()
        try { val result = action(); db.setTransactionSuccessful(); return result }
        finally { db.endTransaction() }
    }

    private fun table(entry: LibraryEntry) = when (entry.kind) {
        LibraryKind.FOLDER -> "bookmark_folders"; LibraryKind.BOOKMARK -> "bookmark_items"
        LibraryKind.HISTORY -> "history"; LibraryKind.OFFLINE -> error("Offline entries are file-backed"); LibraryKind.FAVORITE -> "favorites"
    }
    private fun markBookmarksChanged() = GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.BOOKMARKS)
    private fun like(query: String) = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
    private fun Cursor.string(column: String) = getString(getColumnIndexOrThrow(column))
    private fun Cursor.long(column: String) = getLong(getColumnIndexOrThrow(column))
    private fun NativeBookmarkFolder.entry(hidden: Boolean = false) = LibraryEntry(id, LibraryKind.FOLDER,
        title.orEmpty(), parentId = parentId, position = order.toLong(), createdAt = createdAtSeconds, updatedAt = updatedAtSeconds,
        hidden = hidden)
    private fun moe.via.browser.feature.library.data.NativeBookmarkItem.entry() = LibraryEntry(id, LibraryKind.BOOKMARK,
        title.orEmpty(), url.orEmpty(), folderId, order.toLong(), createdAtSeconds, updatedAt = updatedAtSeconds)
    private fun HistoryRecord.libraryEntry() = LibraryEntry(id.toString(), LibraryKind.HISTORY, title.orEmpty(), url.orEmpty(), updatedAt = updatedAtSeconds)
    private fun containsAsciiNoCase(value: String, query: String): Boolean {
        fun ascii(char: Char) = if (char in 'A'..'Z') char + 32 else char
        if (query.length > value.length) return false
        return (0..value.length - query.length).any { start -> query.indices.all { ascii(value[start + it]) == ascii(query[it]) } }
    }
    override fun close() = store.close()
}
