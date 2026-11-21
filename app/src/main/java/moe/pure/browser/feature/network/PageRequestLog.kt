package moe.pure.browser.feature.network

import android.net.Uri

data class PageRequest(val url: String, val extension: String, val blocked: Boolean,
    val resource: Boolean, val time: Long = System.currentTimeMillis())

/** GP e8.j/d8.c keeps one last-started page log per tab, rather than one log per WebView. */
class PageRequestLog {
    private val requests = mutableListOf<PageRequest>()
    private var lastStartedPageId = -1
    private var currentPageId = -1

    /** Selecting an older slot hides this log; it does not replace or clear the recorded page. */
    @Synchronized fun selectPage(pageId: Int) { currentPageId = pageId }

    @Synchronized fun pageStarted(pageId: Int) {
        requests.clear()
        lastStartedPageId = pageId
    }

    @Synchronized fun record(pageId: Int, url: String, blocked: Boolean = false, range: String? = null) {
        if (pageId != lastStartedPageId) return
        if (Uri.parse(url).scheme?.lowercase() !in setOf("http", "https")) return
        val extension = ResourceTypes.extension(url)
        val resource = ResourceTypes.isResource(extension) || range?.startsWith("bytes=0-") == true
        requests += PageRequest(url, extension, blocked, resource)
    }

    @Synchronized fun snapshot(): List<PageRequest> = if (currentPageId == lastStartedPageId) requests.toList() else emptyList()
    @Synchronized fun hasResources() = currentPageId == lastStartedPageId && requests.any { it.resource }
    @Synchronized fun clear() { requests.clear() }
}
