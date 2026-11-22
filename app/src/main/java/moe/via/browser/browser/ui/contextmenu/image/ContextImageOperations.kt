package moe.via.browser.browser.ui.contextmenu.image

internal data class ContextImageResult(val url: String, val image: ContextImage, val operations: Int)

/** c8.tc stores pending page-XHR operations; snapshots remain independent of later bit removal. */
internal class ContextImageOperations {
    private val pending = HashMap<String, Int>()
    fun add(url: String, operations: Int) { if (url.isNotEmpty()) set(url, get(url) or operations) }
    fun get(url: String): Int = pending[url] ?: 0
    fun remove(url: String, operations: Int) { if (url.isNotEmpty()) set(url, get(url) and operations.inv()) }
    fun clear() = pending.clear()
    private fun set(url: String, operations: Int) {
        if (operations == 0) pending.remove(url) else pending[url] = operations
    }
    companion object {
        const val SHARE = 1
        const val SCAN = 2
        const val SAVE = 4
    }
}
