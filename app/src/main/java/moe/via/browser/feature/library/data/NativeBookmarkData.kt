package moe.via.browser.feature.library.data

data class NativeBookmarkFolder(val id: String, val title: String?, val parentId: String = "",
    val order: Int = 0, val createdAtSeconds: Long = 0, val updatedAtSeconds: Long = 0)

data class NativeBookmarkItem(val id: String, val url: String?, val title: String?, val folderId: String = "",
    val order: Int = 0, val createdAtSeconds: Long = 0, val updatedAtSeconds: Long = 0)

data class NativeBookmarkSnapshot(val folders: List<NativeBookmarkFolder>, val items: List<NativeBookmarkItem>)

/** g6.p.l: normalization is a read/model operation, not a rewrite of persisted text. */
object BookmarkText {
    fun clean(value: String?): String? = value?.let { source -> buildString(source.length) {
        for (character in source) when {
            Character.isWhitespace(character) -> if (character != '\r') append(' ')
            character == '\u00a0' -> append(' ')
            else -> append(character)
        }
    } }
}
