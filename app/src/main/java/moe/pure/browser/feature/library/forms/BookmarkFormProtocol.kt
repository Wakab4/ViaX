package moe.pure.browser.feature.library.forms

import android.os.Bundle

internal enum class BookmarkFormKind { BOOKMARK, FOLDER, MOVE }
internal data class BookmarkFormRequest(
    val kind: BookmarkFormKind,
    val id: String? = null,
    val title: String = "",
    val url: String? = null,
    val folder: String = "",
    val favoriteEnabled: Boolean = false,
    val creationEnabled: Boolean = false
) {
    companion object {
        fun read(kind: BookmarkFormKind, args: Bundle?) = BookmarkFormRequest(kind, args?.getString("id"),
            args?.getString("title").orEmpty(), args?.getString("url"),
            args?.getString(when (kind) { BookmarkFormKind.BOOKMARK -> "folderId"; BookmarkFormKind.FOLDER -> "parent_folder_id"; BookmarkFormKind.MOVE -> "selected_id" }).orEmpty(),
            args?.getBoolean("favorite_enabled") == true, args?.getBoolean("creation_enabled") == true)
    }
}
