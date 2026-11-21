package moe.pure.browser.feature.library.popup

import android.os.Bundle
import moe.pure.browser.feature.library.LibraryEntry

data class BookmarkPopupRequest(val id: String? = null, val url: String? = null,
    val title: String? = null, val position: Int = -1) {
    fun toBundle() = Bundle().apply {
        id?.let { putString("id", it) }; url?.let { putString("url", it) }; title?.let { putString("title", it) }
        putInt("position", position)
    }
    companion object {
        fun fromBundle(bundle: Bundle?) = BookmarkPopupRequest(bundle?.getString("id"), bundle?.getString("url"),
            bundle?.getString("title"), bundle?.getInt("position", -1) ?: -1)
    }
}

data class BookmarkPopupFolderRow(val entry: LibraryEntry, val depth: Int, val selected: Boolean)
enum class BookmarkPopupEventKind { SAVED, DELETED, CLOSE, FAILED }
data class BookmarkPopupEvent(val id: Long, val kind: BookmarkPopupEventKind,
    val entry: LibraryEntry?, val position: Int)

data class BookmarkPopupState(val entry: LibraryEntry? = null, val currentFolder: LibraryEntry? = null,
    val folderRows: List<BookmarkPopupFolderRow> = emptyList(), val page: Int = 0, val height: Int = 0,
    val busy: Boolean = false, val loaded: Boolean = false, val event: BookmarkPopupEvent? = null,
    val draftTitle: String = "", val draftUrl: String = "https://", val favorite: Boolean = false, val query: String = "")
