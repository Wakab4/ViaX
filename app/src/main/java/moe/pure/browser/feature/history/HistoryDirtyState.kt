package moe.pure.browser.feature.history

import android.content.Context
import moe.pure.browser.feature.internalpages.GeneratedPageDirtyState

/** w9.n's history bit 16 is process state, independent of whether an asynchronous SQL write succeeds. */
object HistoryDirtyState {
    fun initialize(context: Context) = GeneratedPageDirtyState.initialize(context)
    fun persist(context: Context) = GeneratedPageDirtyState.persist(context)
    fun isDirty(): Boolean = GeneratedPageDirtyState.isDirty(GeneratedPageDirtyState.HISTORY)
    fun markDirty() = GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.HISTORY)
    fun clear() = GeneratedPageDirtyState.clear(GeneratedPageDirtyState.HISTORY)
}
