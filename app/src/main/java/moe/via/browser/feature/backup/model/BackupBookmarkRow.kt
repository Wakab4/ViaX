package moe.via.browser.feature.backup.model

/** z8.p.d/e payload: escaped folder-name path, Native Int ordering and epoch seconds; no persistent UUID field. */
data class BackupBookmarkRow(
    val title: String?, val url: String?, val folder: String? = "", val order: Int = 0,
    val createdAtSeconds: Long = 0, val updatedAtSeconds: Long = 0
)
