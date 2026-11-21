package moe.pure.browser.feature.library.ui.recycler

import android.content.Context
import moe.pure.browser.ui.components.recycler.NativeRecyclerView

/** Native u0 owns a fast-scroll delegate. Compact dialogs deliberately use a plain RecyclerView. */
class LibraryRecyclerView(context: Context) : NativeRecyclerView(context) {
    init {
        id = android.R.id.list
        NativeRecyclerEffects.install(this)
    }
    override fun onDetachedFromWindow() { cancelFastScroll(); super.onDetachedFromWindow() }
}
