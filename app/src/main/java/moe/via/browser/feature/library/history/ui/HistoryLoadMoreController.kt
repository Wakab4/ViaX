package moe.via.browser.feature.library.history.ui

import androidx.recyclerview.widget.RecyclerView
import java.lang.ref.WeakReference

internal enum class HistoryTailState(val nativeValue: Int) { LOADING(0), READY(1), END(2) }

/** e6.c triggers only at the bottom while idle. The posted branch deliberately has the same second check. */
internal class HistoryLoadMoreController(private val view: RecyclerView, private val changed: (HistoryTailState) -> Unit, private val load: () -> Unit) : RecyclerView.OnScrollListener(), AutoCloseable {
    private var scrolling = false
    private var closed = false
    var active = false
    private var state = HistoryTailState.READY
    init { view.addOnScrollListener(this); changed(state) }
    fun hasMore(value: Boolean) { state = if (value) HistoryTailState.READY else HistoryTailState.END; changed(state) }
    private fun request() {
        if (closed || !active || state != HistoryTailState.READY) return
        state = HistoryTailState.LOADING; changed(state); load()
    }
    override fun onScrollStateChanged(rv: RecyclerView, newState: Int) {
        scrolling = newState != RecyclerView.SCROLL_STATE_IDLE
        if (active && !scrolling && !rv.canScrollVertically(1)) request()
    }
    override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
        if (!active || scrolling || dy == 0 || state != HistoryTailState.READY || rv.canScrollVertically(1)) return
        val reference = WeakReference(rv)
        rv.post { if (reference.get() != null) request() }
    }
    override fun close() { closed = true; view.removeOnScrollListener(this) }
}
