package moe.pure.browser.feature.library.ui.recycler

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Native caches the first attached child, including its physical top, rather than a visible-item estimate. */
internal data class RecyclerScrollState(val position: Int = 0, val offset: Int = 0) {
    fun restore(view: RecyclerView) {
        (view.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(position, offset)
    }

    companion object {
        fun capture(view: RecyclerView): RecyclerScrollState {
            val manager = view.layoutManager as? LinearLayoutManager
            val child = manager?.getChildAt(0)
            return RecyclerScrollState(child?.let { manager?.getPosition(it) } ?: 0,
                child?.top ?: 0)
        }
    }
}
