package moe.pure.browser.ui.tabs.closed

import android.os.SystemClock
import moe.pure.browser.feature.library.ui.recycler.RecyclerScrollState

internal data class ClosedTabsState(val query: String = "", val scroll: RecyclerScrollState = RecyclerScrollState())

/** CLOSED_TABS_CACHE stores only the original query/first-child anchor for 180 seconds. */
internal object ClosedTabsProcessState {
    private var saved: ClosedTabsState? = null
    private var expires = 0L
    fun read(): ClosedTabsState = if (SystemClock.elapsedRealtime() / 1000 <= expires) saved ?: ClosedTabsState() else ClosedTabsState()
    fun save(state: ClosedTabsState) { saved = state; expires = SystemClock.elapsedRealtime() / 1000 + 180 }
}
