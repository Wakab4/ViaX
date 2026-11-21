package moe.pure.browser.feature.catalog

import android.content.Context
import moe.pure.browser.feature.internalpages.GeneratedPageDirtyState

/** w9.n.i/t operate on bit 64 of the same state word used by the other generated pages. */
object CatalogDirtyState {
    fun initialize(context: Context) = GeneratedPageDirtyState.initialize(context)
    fun persist(context: Context) = GeneratedPageDirtyState.persist(context)
    fun isDirty(): Boolean = GeneratedPageDirtyState.isDirty(GeneratedPageDirtyState.CATALOG)
    fun markDirty() = GeneratedPageDirtyState.markDirty(GeneratedPageDirtyState.CATALOG)
    fun clear() = GeneratedPageDirtyState.clear(GeneratedPageDirtyState.CATALOG)
}
