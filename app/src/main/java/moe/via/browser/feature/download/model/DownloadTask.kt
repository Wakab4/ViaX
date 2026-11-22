package moe.via.browser.feature.download.model

enum class DownloadState { QUEUED, RUNNING, PUBLISHING, PAUSED, COMPLETE, FAILED, CANCELED, WAITING_NETWORK }

data class DownloadTask(
    val id: Long = 0,
    val url: String,
    val name: String,
    val target: String,
    val mime: String = "application/octet-stream",
    val headers: Map<String, String> = emptyMap(),
    val state: DownloadState = DownloadState.QUEUED,
    val total: Long = -1,
    val received: Long = 0,
    val etag: String? = null,
    val lastModified: String? = null,
    val connections: Int = 1,
    val error: String? = null,
    val created: Long = System.currentTimeMillis(),
    val updated: Long = created,
    val finished: Long = 0,
    val speed: Long = 0,
    val payload: String? = null,
    val nativeFlags: Int = 1
) {
    val rangeMode: Int get() = if (nativeFlags and 2 == 0) -1 else if (nativeFlags and 1 != 0) 1 else 0
    val allowMimeChange: Boolean get() = nativeFlags and 4 != 0
}

data class DownloadPart(val index: Int, val start: Long, val end: Long, val received: Long = 0) {
    val length get() = if (end < 0) -1 else end - start + 1
    val complete get() = length >= 0 && received == length
}

data class DownloadMetadata(val total: Long, val ranges: Boolean, val etag: String?, val lastModified: String?, val mime: String?, val ready: Boolean = true)
