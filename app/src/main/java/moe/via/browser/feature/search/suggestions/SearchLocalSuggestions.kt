package moe.via.browser.feature.search.suggestions

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import moe.via.browser.storage.database.BrowserDatabase
import moe.via.browser.feature.home.data.NativeFavoriteRows
import moe.via.browser.feature.internalpages.GeneratedAddress
import moe.via.browser.feature.library.data.BookmarkFolderVisibility
import moe.via.browser.feature.search.model.SearchSuggestion
import moe.via.browser.feature.search.model.SearchSuggestionType
import moe.via.browser.feature.search.model.SearchTab
import moe.via.browser.feature.search.resolve.SearchUrlResolver
import java.io.Closeable

/** Actual bound queries against the favorites, bookmark_items and history schemas. */
internal class SearchLocalSuggestions(context: Context) : Closeable {
    private val store = BrowserDatabase(context.applicationContext)
    private val folderVisibility = BookmarkFolderVisibility(context)
    fun query(query: String, tabs: List<SearchTab>, mask: Int): List<SearchSuggestion> {
        val text = query.trim()
        if (text.isEmpty()) return if (mask and 32 != 0) SearchHistory.suggestions() else emptyList()
        if (text.length >= 10000) return emptyList()
        val links = SearchUrlResolver.httpLinks(text).map { url ->
            SearchSuggestion(SearchSuggestionType.SEARCH, title = url.substringAfter("://").substringBefore('/'), url = url)
        }
        if (text.length >= 500 || mask and 15 == 0) return links
        val local = mutableListOf<SearchSuggestion>()
        if (mask and 4 != 0) local.addAll(tabs.asSequence().filter {
            SearchUrlResolver.isHttp(it.url) && (it.title.contains(text) || text.length > 4 && it.url.contains(text))
        }.take(5).map { SearchSuggestion(SearchSuggestionType.TAB, it.title, it.url, tabId = it.id) }.toList())
        if (mask and 11 != 0) {
            val db = store.readableDatabase
            val hiddenFolders = if (mask and 3 != 0) folderVisibility.excludedIds(db) else emptySet()
            if (mask and 1 != 0) {
                val rows = NativeFavoriteRows(db).search(text, if (hiddenFolders.isEmpty()) 5 else 0)
                local.addAll(rows.asSequence().filterNot { GeneratedAddress.folder(GeneratedAddress.normalize(it.url)) in hiddenFolders }
                    .take(5).map { SearchSuggestion(SearchSuggestionType.FAVORITE, it.title, it.url) })
            }
            if (mask and 2 != 0) local.addAll(read(db, "bookmark_items", text, SearchSuggestionType.BOOKMARK, null, hiddenFolders))
            if (mask and 8 != 0) local.addAll(read(db, "history", text, SearchSuggestionType.HISTORY, "updated_at DESC"))
        }
        val seen = hashSetOf<String>()
        return links + local.filter { it.url.isEmpty() || seen.add(it.url) }
    }

    private fun read(db: SQLiteDatabase, table: String, query: String, type: Int, order: String?,
        hiddenFolders: Set<String> = emptySet()): List<SearchSuggestion> {
        val escaped = '%' in query || '_' in query
        val needle = if (escaped) query.replace("%", "\\%").replace("_", "\\_") else query
        val escape = if (escaped) " ESCAPE '\\'" else ""
        val selection: String
        val args: Array<String>
        if (SearchUrlResolver.isHttp(needle)) {
            selection = "url LIKE ?$escape"
            args = arrayOf("$needle%")
        } else {
            selection = "url LIKE ?$escape OR title LIKE ?$escape OR title LIKE ?$escape"
            args = arrayOf("%_$needle%", "%_$needle%", "$needle%")
        }
        val filterFolders = hiddenFolders.isNotEmpty()
        val columns = if (filterFolders) arrayOf("title", "url", "folder_id") else arrayOf("title", "url")
        return db.query(table, columns, selection, args, null, null, order, if (filterFolders) null else "5").use { cursor ->
            buildList {
                while (size < 5 && cursor.moveToNext()) {
                    if (filterFolders && cursor.getString(2) in hiddenFolders) continue
                    add(SearchSuggestion(type, cursor.getString(0).orEmpty(), cursor.getString(1).orEmpty()))
                }
            }
        }
    }
    override fun close() = store.close()
}
