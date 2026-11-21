package moe.pure.browser.feature.sync.model

enum class SyncKind { VIA_CLOUD, WEBDAV }
enum class SyncMode { MERGE, PUSH_LOCAL, PULL_REMOTE }
enum class SyncData(val bit: Int, val fileName: String, val mime: String) {
    BOOKMARKS(16, "bookmarks.html", "text/html"),
    FAVORITES(64, "favorites.txt", "text/html"),
    SETTINGS(32, "settings.txt", "text/plain")
}

data class WebDavProfile(
    val id: String,
    val baseUrl: String,
    val username: String,
    val remotePath: String = "/Via",
    val digestAuth: Boolean = false,
    val flags: Int = 81,
    val lastSuccessAt: Long = 0,
    val configured: Boolean = true
) {
    val autoSync get() = flags and 1 != 0
    val data get() = SyncData.entries.filterTo(linkedSetOf()) { flags and it.bit != 0 }
    override fun toString() = "WebDavProfile(id=$id, flags=$flags)"
}

data class CloudAccount(val serverIndex: Int, val username: String, val loggedIn: Boolean) {
    override fun toString() = "CloudAccount(serverIndex=$serverIndex, loggedIn=$loggedIn)"
}
data class CloudLoginResult(val account: CloudAccount, val registered: Boolean)
class CloudBookmarkTooLargeException : Exception("书签数据过大，无法上传。是否继续上传其他数据？")
data class SyncFileState(val profileId: String, val path: String, val remoteModified: Long = 0, val lastSynced: Long = 0)
enum class SyncPhase { CHECKING, DOWNLOADING, IMPORTING, EXPORTING, UPLOADING, COMPLETE }
data class SyncProgress(val data: SyncData?, val phase: SyncPhase, val completed: Int, val total: Int)
data class SyncItemResult(val data: SyncData, val success: Boolean, val errorCode: Int = 0, val error: String? = null)
data class SyncReport(val items: List<SyncItemResult> = emptyList(), val skipped: Boolean = false, val cancelled: Boolean = false, val error: String? = null) {
    val success get() = !cancelled && error == null && items.all { it.success }
    val errors get() = items.filterNot { it.success }
}
interface SyncCall { val isCancelled: Boolean; val isDone: Boolean; fun cancel() }

/** Plain credentials are scoped to an explicitly requested operation and never printed. */
internal data class SyncCredentials(val username: String, val password: String) {
    override fun toString() = "SyncCredentials(<redacted>)"
}
