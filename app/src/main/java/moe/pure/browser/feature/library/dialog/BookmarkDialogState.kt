package moe.pure.browser.feature.library.dialog

import android.os.SystemClock

/** Native BOOKMARK_DIALOG_CACHE is process memory, with whole-second expiry and a 180 second life. */
internal object BookmarkDialogState {
    data class Scroll(val position: Int = 0, val offset: Int = 0)
    data class Saved(val folder: String, val scroll: Scroll, val expires: Long)
    private var saved: Saved? = null
    fun read(): Saved? {
        val current = saved ?: return null
        if (current.expires < SystemClock.elapsedRealtime() / 1000) { saved = null; return null }
        return current
    }
    fun save(folder: String, scroll: Scroll) {
        saved = Saved(folder, scroll, SystemClock.elapsedRealtime() / 1000 + 180)
    }
}
