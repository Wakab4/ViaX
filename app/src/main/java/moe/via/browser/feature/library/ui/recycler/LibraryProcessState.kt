package moe.via.browser.feature.library.ui.recycler

import android.os.SystemClock
import moe.via.browser.feature.library.LibraryTab
import moe.via.browser.ui.components.recycler.RecyclerScrollState

/** BOOKMARK_CACHE/HISTORY_CACHE are separate process-memory entries; current folders are not durable state. */
internal object LibraryProcessState {
    data class Saved(val folder: String, val query: String, val scroll: RecyclerScrollState, val scrollY: Int, val expires: Long)
    private val values = mutableMapOf<LibraryTab, Saved>()
    fun read(tab: LibraryTab): Saved? = values[tab]?.takeIf { it.expires >= SystemClock.elapsedRealtime() / 1000 }
        .also { if (it == null) values.remove(tab) }
    fun save(tab: LibraryTab, folder: String, query: String, scroll: RecyclerScrollState, scrollY: Int) {
        values[tab] = Saved(folder, query, scroll, scrollY, SystemClock.elapsedRealtime() / 1000 + 180)
    }
}
