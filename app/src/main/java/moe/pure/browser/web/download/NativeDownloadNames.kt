package moe.pure.browser.web.download

import android.os.SystemClock

/** v5.b's process-wide dl bundle: each write replaces the previous URL/name and expires in60 seconds. */
internal object NativeDownloadNames {
    private data class Entry(val url: String, val name: String, val expires: Long)
    @Volatile private var entry: Entry? = null
    fun remember(url: String, name: String) {
        if (url.isNotEmpty() && name.isNotEmpty()) entry = Entry(url, name, SystemClock.elapsedRealtime() / 1000 + 60)
    }
    fun find(url: String?): String? {
        val value = entry ?: return null
        if (value.expires < SystemClock.elapsedRealtime() / 1000) { entry = null; return null }
        return value.name.takeIf { value.url == url }
    }
}
