package moe.pure.browser.ui.components.recycler

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.ui.components.dp
import kotlin.math.abs

/** n8.b's range/restore selection, checkbox edge capture and 25 ms edge scrolling. */
internal class NativeRangeSelection(private val list: RecyclerView, private val actions: Actions,
    private val stateChanged: (Boolean) -> Unit = {}) : RecyclerView.OnItemTouchListener, AutoCloseable {
    interface Actions {
        fun selectable(position: Int): Boolean
        fun selected(position: Int): Boolean
        fun select(position: Int, value: Boolean)
    }
    private var active = false
    private var edgeDown = -1f
    private var anchor = -1
    private var previous = -1
    private var minimum = -1
    private var maximum = -1
    private var scroll = 0
    private val originals = mutableMapOf<Int, Boolean>()
    private val edge = list.dp(56)
    private val selectWidth = list.dp(48)
    private val rtl = list.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
    private val tick = object : Runnable {
        override fun run() { if (active && scroll != 0) { list.scrollBy(0, scroll); list.postDelayed(this, 25) } }
    }
    init { list.addOnItemTouchListener(this) }
    private fun checkboxEdge(x: Float) = if (rtl) x < selectWidth else x > list.width - selectWidth
    private fun position(event: MotionEvent) = list.findChildViewUnder(event.x, event.y)?.let(list::getChildAdapterPosition) ?: RecyclerView.NO_POSITION
    fun start(position: Int): Boolean {
        if (active || !actions.selectable(position)) return false
        finish()
        actions.select(position, !actions.selected(position))
        active = true; anchor = position; previous = position
        stateChanged(true)
        return true
    }
    override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
        if ((rv.adapter?.itemCount ?: 0) == 0) { finish(); return false }
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            val intercepted = active
            finish(); edgeDown = -1f
            return intercepted
        }
        if (!active) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) edgeDown = if (checkboxEdge(event.x)) event.y else -1f
            else if (edgeDown >= 0f && event.actionMasked == MotionEvent.ACTION_MOVE && abs(event.y - edgeDown) > ViewConfiguration.get(rv.context).scaledTouchSlop) {
                val at = position(event)
                if (at == RecyclerView.NO_POSITION || !start(at)) edgeDown = -1f
            }
        }
        return active
    }
    override fun onTouchEvent(rv: RecyclerView, event: MotionEvent) {
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) { finish(); return }
        if (event.actionMasked != MotionEvent.ACTION_MOVE) return
        val nextScroll = when {
            event.y <= edge -> -minOf(edge.toFloat(), edge - event.y).toInt() / 2
            event.y >= rv.measuredHeight - edge -> minOf(edge.toFloat(), event.y - (rv.measuredHeight - edge)).toInt() / 2
            else -> 0
        }
        if (nextScroll != scroll) {
            scroll = nextScroll; rv.removeCallbacks(tick)
            if (scroll != 0) rv.postDelayed(tick, 25)
        }
        val at = position(event)
        if (at == RecyclerView.NO_POSITION || at == previous || (edgeDown > 0 && !checkboxEdge(event.x))) return
        previous = at
        if (minimum == -1) minimum = at
        if (maximum == -1) maximum = at
        minimum = minOf(minimum, at); maximum = maxOf(maximum, at)
        val selected = actions.selected(anchor)
        if (at == anchor) {
            for (index in minimum..maximum) if (index != anchor) set(index, originals[index] == true)
            minimum = at; maximum = at
        } else {
            val first = minOf(anchor, at); val last = maxOf(anchor, at)
            for (index in first..last) { originals.putIfAbsent(index, actions.selected(index)); set(index, selected) }
            for (index in minimum until first) set(index, originals[index] == true)
            for (index in last + 1..maximum) set(index, originals[index] == true)
        }
    }
    private fun set(position: Int, selected: Boolean) = actions.select(position, selected && actions.selectable(position))
    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
    fun finish() { val changed = active; active = false; originals.clear(); anchor = -1; previous = -1; minimum = -1; maximum = -1; scroll = 0; list.removeCallbacks(tick); if (changed) stateChanged(false) }
    override fun close() { finish(); list.removeOnItemTouchListener(this) }
}
