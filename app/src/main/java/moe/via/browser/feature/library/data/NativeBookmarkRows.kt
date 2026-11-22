package moe.via.browser.feature.library.data

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import moe.via.browser.core.logging.NativeLog as Log
import java.util.UUID

/** o9.g/e: actual TEXT identities, second metadata, and exact-URL replacement. */
class NativeBookmarkRows(private val db: SQLiteDatabase) {
    private val cache = NativeBookmarkFolderCache.forDatabase(db)

    fun readAll(): NativeBookmarkSnapshot = NativeBookmarkSnapshot(folders(), items())

    fun folders(): List<NativeBookmarkFolder> = synchronized(cache) {
        if (cache.complete) return@synchronized cache.values.values.toList()
        cache.complete = true
        val rows = queryFolders(null, null)
        rows.forEach { cache.values[it.id] = it }
        rows
    }

    fun folder(id: String): NativeBookmarkFolder? {
        if (id.isEmpty()) return NativeBookmarkFolder("", null)
        return synchronized(cache) {
            cache.values[id] ?: queryFolders("_id=?", arrayOf(id), "ordering", "1").firstOrNull()
                ?.also { cache.values[it.id] = it }
        }
    }

    fun children(parent: String): List<NativeBookmarkFolder> = synchronized(cache) {
        if (cache.complete) cache.values.values.filter { it.parentId == parent }
        else queryFolders("parent_folder_id=?", arrayOf(parent)).also { rows -> rows.forEach { cache.values[it.id] = it } }
    }

    fun folderByTitle(title: String): NativeBookmarkFolder? {
        if (title.isEmpty()) return null
        return synchronized(cache) {
            cache.values.values.firstOrNull { title.equals(it.title, true) }
                ?: queryFolders("title=?", arrayOf(title), "ordering", "1").firstOrNull()
                    ?.also { cache.values[it.id] = it }
        }
    }

    fun items(folder: String? = null): List<NativeBookmarkItem> = queryItems(
        folder?.let { "folder_id=?" }, folder?.let { arrayOf(it) })

    fun item(id: String): NativeBookmarkItem? = queryItems("_id=?", arrayOf(id), "ordering", "1").firstOrNull()
    fun itemByUrl(urls: List<String>): NativeBookmarkItem? = if (urls.isEmpty()) null else
        queryItems("url IN (${urls.joinToString(",") { "?" }})", urls.toTypedArray(), "ordering", "1").firstOrNull()

    /** o9.g.z: Java trim, two unescaped LIKE patterns, no forced ordering or row limit. */
    fun searchFolders(raw: String): List<NativeBookmarkFolder> {
        val text = raw.trim { it <= ' ' }
        if (text.isEmpty()) return emptyList()
        return queryFolders("title LIKE ? OR title LIKE ?", arrayOf("%_$text%", "$text%"))
    }

    fun addFolder(title: String, parent: String, nowSeconds: Long, normalizeTitle: Boolean = true): String =
        addFolderRecord(title, parent, nowSeconds, normalizeTitle)?.id.orEmpty()

    fun addFolderRecord(title: String, parent: String, nowSeconds: Long, normalizeTitle: Boolean = true): NativeBookmarkFolder? {
        var stored: NativeBookmarkFolder? = null
        val id = transaction {
            val value = NativeBookmarkFolder(UUID.randomUUID().toString(), if (normalizeTitle) BookmarkText.clean(title) else title, parent,
                nextOrder("bookmark_folders"), nowSeconds, nowSeconds)
            if (db.insert("bookmark_folders", null, values(value)) <= 0) ""
            else value.id.also { stored = value }
        } ?: ""
        stored?.takeIf { id.isNotEmpty() }?.let { value -> synchronized(cache) { cache.values[id] = value } }
        return stored?.takeIf { id.isNotEmpty() }
    }

    fun updateFolder(id: String, title: String, parent: String, nowSeconds: Long): String {
        val original = folder(id)?.takeUnless { it.id.isEmpty() } ?: return ""
        val value = original.copy(title = BookmarkText.clean(title), parentId = parent, updatedAtSeconds = nowSeconds)
        return try {
            if (db.update("bookmark_folders", values(value, includeId = false), "_id=?", arrayOf(id)) <= 0) ""
            else id.also { synchronized(cache) { cache.values[id] = value } }
        } catch (error: Exception) { failure(error); "" }
    }

    fun addItem(title: String, url: String, folder: String, nowSeconds: Long): String =
        addItemRecord(title, url, folder, nowSeconds)?.id.orEmpty()

    fun addItemRecord(title: String, url: String, folder: String, nowSeconds: Long): NativeBookmarkItem? {
        if (!validUrl(url)) return null
        return transaction {
            db.delete("bookmark_items", "url=?", arrayOf(url))
            val value = NativeBookmarkItem(UUID.randomUUID().toString(), url, BookmarkText.clean(title), folder,
                nextOrder("bookmark_items", folder), nowSeconds, nowSeconds)
            if (db.insert("bookmark_items", null, values(value)) > 0) value else null
        }
    }

    fun updateItem(id: String, title: String, url: String, folder: String, nowSeconds: Long): String {
        if (!validUrl(url)) return ""
        val original = item(id) ?: return ""
        val value = original.copy(title = BookmarkText.clean(title), url = url, folderId = folder, updatedAtSeconds = nowSeconds)
        return if (updateItemRecord(value)) id else ""
    }

    fun updateItemRecord(value: NativeBookmarkItem): Boolean {
        if (value.id.isEmpty() || !validUrl(value.url)) return false
        return try { db.update("bookmark_items", values(value, includeId = false), "_id=?", arrayOf(value.id)) > 0 }
        catch (error: Exception) { failure(error); false }
    }

    /** o9.e.z first applies z8.p.p's folder-title mapping, then o9.g.x's delete/insert batch. */
    fun importBatch(snapshot: NativeBookmarkSnapshot): Int {
        if (snapshot.folders.isEmpty() && snapshot.items.isEmpty()) return 0
        val mapped = mergeFolders(snapshot, folders())
        val result = transaction {
            var count = 0
            for (folder in mapped.folders) if (folder.id.isNotEmpty()) {
                db.delete("bookmark_folders", "_id=?", arrayOf(folder.id))
                db.insert("bookmark_folders", null, values(folder))
                count++
            }
            for (item in mapped.items) if (item.id.isNotEmpty() && validUrl(item.url)) {
                db.delete("bookmark_items", "url=?", arrayOf(item.url!!))
                db.insert("bookmark_items", null, values(item))
                count++
            }
            count
        } ?: 0
        if (result > 0) synchronized(cache) { mapped.folders.forEach { cache.values[it.id] = it } }
        return result
    }

    fun clear() {
        db.delete("bookmark_items", null, null)
        db.delete("bookmark_folders", null, null)
        synchronized(cache) { cache.complete = true; cache.values.clear() }
    }

    fun deleteItems(ids: List<String>): Int = if (ids.isEmpty()) 0 else
        db.delete("bookmark_items", "_id IN (${marks(ids.size)})", ids.toTypedArray())
    fun deleteUrl(url: String): Int = db.delete("bookmark_items", "url=?", arrayOf(url))

    fun descendants(id: String): List<NativeBookmarkFolder> {
        val start = folder(id) ?: return emptyList()
        val remaining = folders().toMutableList()
        val result = mutableListOf(start)
        val seen = hashSetOf(id)
        var index = 0
        while (index < result.size) {
            val parent = result[index++].id
            for (position in remaining.indices.reversed()) if (remaining[position].parentId == parent) {
                val value = remaining.removeAt(position)
                if (seen.add(value.id)) result.add(index, value)
            }
        }
        return result
    }

    fun deleteFolders(ids: List<String>) {
        val all = ids.flatMap(::descendants).map { it.id }.filter { it.isNotEmpty() }.toSet().toList()
        if (all.isEmpty()) return
        db.delete("bookmark_folders", "_id IN (${marks(all.size)})", all.toTypedArray())
        db.delete("bookmark_items", "folder_id IN (${marks(all.size)})", all.toTypedArray())
        synchronized(cache) { all.forEach(cache.values::remove) }
    }

    fun moveFolder(id: String, parent: String, order: Int, nowSeconds: Long) {
        val old = folder(id) ?: return
        val next = old.copy(parentId = parent, order = order, updatedAtSeconds = nowSeconds)
        if (db.update("bookmark_folders", values(next, false), "_id=?", arrayOf(id)) > 0)
            synchronized(cache) { cache.values[id] = next }
    }
    fun moveItem(id: String, parent: String, order: Int, nowSeconds: Long) {
        db.update("bookmark_items", ContentValues().apply {
            put("folder_id", parent); put("ordering", order); put("last_updated_at", nowSeconds)
        }, "_id=?", arrayOf(id))
    }
    fun reorderFolder(id: String, order: Int) {
        db.update("bookmark_folders", ContentValues().apply { put("ordering", order) }, "_id=?", arrayOf(id))
        synchronized(cache) { cache.values[id]?.let { cache.values[id] = it.copy(order = order) } }
    }
    fun reorderItem(id: String, order: Int) { db.update("bookmark_items", ContentValues().apply { put("ordering", order) }, "_id=?", arrayOf(id)) }

    fun nextItemOrder(folder: String) = nextOrder("bookmark_items", folder)
    fun nextFolderOrder() = nextOrder("bookmark_folders")

    private fun nextOrder(table: String, folder: String? = null): Int = db.rawQuery(
        "SELECT MAX(ordering) FROM $table" + if (folder == null) "" else " WHERE folder_id=?",
        folder?.let { arrayOf(it) }).use { if (it.moveToFirst()) it.getInt(0) + 1 else 0 }

    private fun queryFolders(selection: String?, args: Array<String>?, order: String? = null, limit: String? = null): List<NativeBookmarkFolder> {
        val rows = mutableListOf<NativeBookmarkFolder>()
        try {
            db.query("bookmark_folders", null, selection, args, null, null, order, limit).use { cursor ->
                while (cursor.moveToNext()) rows.add(NativeBookmarkFolder(cursor.text("_id").orEmpty(),
                    BookmarkText.clean(cursor.text("title")), cursor.text("parent_folder_id").orEmpty(), cursor.number("ordering").toInt(),
                    cursor.number("created_at"), cursor.number("last_updated_at")))
            }
        } catch (error: Exception) { failure(error) }
        return rows
    }
    private fun queryItems(selection: String?, args: Array<String>?, order: String? = null, limit: String? = null): List<NativeBookmarkItem> {
        val rows = mutableListOf<NativeBookmarkItem>()
        try {
            db.query("bookmark_items", null, selection, args, null, null, order, limit).use { cursor ->
                while (cursor.moveToNext()) rows.add(NativeBookmarkItem(cursor.text("_id").orEmpty(), cursor.text("url"),
                    BookmarkText.clean(cursor.text("title")), cursor.text("folder_id").orEmpty(), cursor.number("ordering").toInt(),
                    cursor.number("created_at"), cursor.number("last_updated_at")))
            }
        } catch (error: Exception) { failure(error) }
        return rows
    }

    private fun <T> transaction(block: () -> T): T? {
        val own = !db.inTransaction()
        if (own) db.beginTransaction()
        return try { block().also { if (own) db.setTransactionSuccessful() } }
        catch (error: Exception) {
            failure(error)
            if (!own) throw error
            null
        }
        finally { if (own) db.endTransaction() }
    }

    companion object {
        private fun marks(size: Int) = List(size) { "?" }.joinToString(",")
        private fun validUrl(value: String?) = !value.isNullOrEmpty() && value.length <= 1_048_576
        private fun failure(error: Exception) { Log.warn("Bookmarks", "Native bookmark operation", error) }
        private fun Cursor.text(name: String): String? = getString(getColumnIndexOrThrow(name))
        private fun Cursor.number(name: String) = getLong(getColumnIndexOrThrow(name))
        internal fun values(value: NativeBookmarkFolder, includeId: Boolean = true) = ContentValues().apply {
            if (includeId) put("_id", value.id)
            put("title", value.title); put("parent_folder_id", value.parentId); put("ordering", value.order)
            put("created_at", value.createdAtSeconds); put("last_updated_at", value.updatedAtSeconds)
        }
        internal fun values(value: NativeBookmarkItem, includeId: Boolean = true) = ContentValues().apply {
            if (includeId) put("_id", value.id)
            put("url", value.url); put("title", value.title); put("folder_id", value.folderId); put("ordering", value.order)
            put("created_at", value.createdAtSeconds); put("last_updated_at", value.updatedAtSeconds)
        }

        private fun mergeFolders(input: NativeBookmarkSnapshot, existing: List<NativeBookmarkFolder>): NativeBookmarkSnapshot {
            val folders = input.folders.toMutableList()
            val items = input.items.toMutableList()
            val visited = hashSetOf<String>()
            fun merge(parent: String) {
                if (!visited.add(parent)) return
                val titles = HashMap<String, String>()
                existing.filter { it.parentId == parent && it.title != null }.forEach { titles[it.title!!] = it.id }
                if (titles.isEmpty()) return
                val mapping = HashMap<String, String>()
                folders.filter { it.parentId == parent && it.title != null }.forEach { folder ->
                    folder.title?.let(titles::get)?.let { mapping[folder.id] = it }
                }
                if (mapping.isEmpty()) return
                for (index in items.indices) mapping[items[index].folderId]?.let { items[index] = items[index].copy(folderId = it) }
                val changed = mutableListOf<String>()
                for (index in folders.indices) {
                    val value = folders[index]
                    val id = mapping[value.id]
                    val above = mapping[value.parentId]
                    if (id != null || above != null) {
                        val next = value.copy(id = id ?: value.id, parentId = above ?: value.parentId)
                        folders[index] = next; changed.add(next.id)
                    }
                }
                changed.forEach(::merge)
            }
            merge("")
            return NativeBookmarkSnapshot(folders, items)
        }
    }
}
