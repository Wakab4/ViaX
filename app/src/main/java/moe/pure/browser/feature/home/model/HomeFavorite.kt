package moe.pure.browser.feature.home.model

/** n9.b retains raw nulls; its public URL/title getters deliberately return an empty string. */
class HomeFavorite(
    var id: Int = 0,
    url: String? = null,
    title: String? = null,
    var folder: String? = null,
    var position: Int = -1,
    var updatedAt: Long = 0,
    var createdAt: Long = 0
) {
    var rawUrl: String? = url
    var rawTitle: String? = title
    val url: String get() = rawUrl.orEmpty()
    val title: String get() = rawTitle.orEmpty()
    val hasEmptyUrl: Boolean get() = rawUrl.isNullOrEmpty()

    override fun equals(other: Any?): Boolean = this === other || other is HomeFavorite &&
        id == other.id && position == other.position && createdAt == other.createdAt && updatedAt == other.updatedAt &&
        rawUrl == other.rawUrl && rawTitle == other.rawTitle && folder == other.folder

    override fun hashCode(): Int {
        var result = id
        result = result * 31 + (rawUrl?.hashCode() ?: 0)
        result = result * 31 + (rawTitle?.hashCode() ?: 0)
        result = result * 31 + (folder?.hashCode() ?: 0)
        result = result * 31 + position
        result = result * 31 + createdAt.hashCode()
        return result * 31 + updatedAt.hashCode()
    }

    override fun toString(): String = "HistoryItem{id=$id, url='$rawUrl', title='$rawTitle', folder='$folder', order=$position, createdAt=$createdAt, updatedAt=$updatedAt}"
}

/** Text has tags stripped, href is the Native jump route, and iconPath is relative to the local document. */
data class HomeFavoriteTile(val href: String, val title: String, val letter: String, val iconPath: String, val color: String)
