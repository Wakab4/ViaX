package moe.pure.browser.feature.library.format

import moe.pure.browser.feature.backup.model.BackupBookmarkRow
import moe.pure.browser.feature.library.data.BookmarkText
import moe.pure.browser.feature.library.data.NativeBookmarkFolder
import moe.pure.browser.feature.library.data.NativeBookmarkItem
import moe.pure.browser.feature.library.data.NativeBookmarkSnapshot
import java.util.UUID

/** z8.p.d/e are the legacy flat-row format, separate from Netscape HTML's parent stack. */
internal object BookmarkRowFormat {
    fun export(snapshot: NativeBookmarkSnapshot): List<BackupBookmarkRow> {
        val remaining = snapshot.folders.toMutableList()
        val paths = hashMapOf<String, String>()
        val pending = mutableListOf(NativeBookmarkFolder("", null))
        var index = 0
        while (index < pending.size) {
            val parent = pending[index++]
            val prefix = paths[parent.id]?.let { "$it/" }.orEmpty()
            for (position in remaining.lastIndex downTo 0) {
                val folder = remaining[position]
                if (parent.id == folder.parentId) {
                    pending.add(index, folder)
                    paths[folder.id] = prefix + folder.title!!.replace("/", "\\/")
                    remaining.removeAt(position)
                }
            }
        }
        return snapshot.items.sortedWith { left, right ->
            val parent = left.folderId.compareTo(right.folderId, ignoreCase = true)
            if (parent != 0) parent else left.order.compareTo(right.order)
        }.map { BackupBookmarkRow(it.title.orEmpty(), it.url.orEmpty(), paths[it.folderId], it.order, it.createdAtSeconds, it.updatedAtSeconds) }
    }

    fun restore(rows: List<BackupBookmarkRow>): NativeBookmarkSnapshot {
        val folders = mutableListOf<NativeBookmarkFolder>()
        val items = mutableListOf<NativeBookmarkItem>()
        val paths = hashMapOf("" to "")
        var time = System.currentTimeMillis() / 1000 - rows.size
        var folderOrder = 0
        for (row in rows) {
            if (NativeBookmarkHtml.invalidUrl(row.url)) continue
            val id = UUID.randomUUID().toString()
            val created = if (row.createdAtSeconds == 0L) ++time else row.createdAtSeconds
            val path = row.folder.orEmpty()
            if (!paths.containsKey(path)) {
                var start = 0
                while (start < path.length) {
                    var end = path.indexOf('/', start)
                    while (end > 0 && path[end - 1] == '\\') end = path.indexOf('/', end + 1)
                    if (end == -1) end = path.length
                    val prefix = path.substring(0, end)
                    if (!paths.containsKey(prefix)) {
                        val above = path.substring(0, maxOf(0, start - 1))
                        val folderId = UUID.randomUUID().toString()
                        val now = System.currentTimeMillis() / 1000
                        folders += NativeBookmarkFolder(folderId, path.substring(start, end).replace("\\/", "/"),
                            paths[above].orEmpty(), ++folderOrder, now, now)
                        paths[prefix] = folderId
                    }
                    start = end + 1
                }
            }
            items += NativeBookmarkItem(id, row.url, BookmarkText.clean(row.title.orEmpty()), paths[path].orEmpty(), row.order,
                created, row.updatedAtSeconds.takeUnless { it == 0L } ?: created)
        }
        return NativeBookmarkSnapshot(folders, items)
    }
}
