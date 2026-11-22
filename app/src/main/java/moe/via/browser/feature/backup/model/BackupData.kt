package moe.via.browser.feature.backup.model

/** Native local-backup bits differ from WebDAV's three-data flags. */
enum class BackupData(val bit: Int, val title: String, val fileName: String) {
    BOOKMARKS(2, "书签", "bookmarks.html"), FAVORITES(4, "主页收藏", "favorites.txt"),
    HISTORY(8, "历史", "history.txt"), CLOSED_TABS(16, "关闭的标签页", "tabs.txt"),
    SETTINGS(1, "设置", "settings.txt"), PASSWORDS(32, "保存的密码", "pass.enc");
    companion object { val DEFAULT = entries.filterTo(linkedSetOf()) { it != PASSWORDS } }
}

enum class ImportMode(val nativeValue: Int) { MERGE(0), REPLACE(1), RECONCILE(2) }
data class BackupItemResult(val data: BackupData, val count: Int = 0, val error: String? = null)
data class BackupResult(val items: List<BackupItemResult>) {
    val errors get() = items.filter { it.error != null }
    val successful get() = items.filter { it.error == null }
}
