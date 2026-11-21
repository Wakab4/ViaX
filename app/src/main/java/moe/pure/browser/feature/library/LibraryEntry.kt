package moe.pure.browser.feature.library

enum class LibraryTab(val label: String) { BOOKMARKS("书签"), HISTORY("历史"), OFFLINE("离线页面") }
enum class LibraryKind { FOLDER, BOOKMARK, HISTORY, OFFLINE, FAVORITE }
enum class BookmarkSort { OLDEST, NEWEST, NAME }

data class LibraryEntry(
    val id: String,
    val kind: LibraryKind,
    val title: String,
    val url: String = "",
    val parentId: String = "",
    val position: Long = 0,
    val createdAt: Long = 0,
    val path: String = "",
    val size: Long = 0,
    /** Bookmark/history use seconds; folders preserve Native caller values, including m0's millis/100. */
    val updatedAt: Long = 0,
    val childCount: Int = 0
) {
    /** Only the independent INTEGER history and favorite stores consume this projection. */
    val numericId: Long get() {
        check(kind == LibraryKind.HISTORY || kind == LibraryKind.FAVORITE)
        return id.toLong()
    }
    val key: String get() = if (kind == LibraryKind.OFFLINE) "$kind:$path" else "$kind:$id"
    val isFolder: Boolean get() = kind == LibraryKind.FOLDER
}
