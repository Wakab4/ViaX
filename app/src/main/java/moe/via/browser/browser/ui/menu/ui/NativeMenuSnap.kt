package moe.via.browser.browser.ui.menu.ui

import android.util.DisplayMetrics
import android.view.View
import android.view.animation.Interpolator
import android.widget.Scroller
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import kotlin.math.abs
import kotlin.math.max

/** i8.a(1): page quantization and RTL anchoring use the real grid's decorated positions. */
internal class NativeMenuSnap(private val changed: (Int) -> Unit) : SnapHelper() {
    private var recycler: RecyclerView? = null
    private var helper: OrientationHelper? = null
    private var scroller: Scroller? = null
    private var rtl = false
    private var unit = 0
    private var pageItems = 0
    private var previousFirst = -1

    override fun attachToRecyclerView(recyclerView: RecyclerView?) {
        recycler = recyclerView
        if (recyclerView != null) {
            val manager = recyclerView.layoutManager as? LinearLayoutManager ?: error("RecyclerView must have a layout manager")
            rtl = manager.canScrollHorizontally() && recyclerView.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
            helper = if (manager.canScrollHorizontally()) OrientationHelper.createHorizontalHelper(manager)
                else OrientationHelper.createVerticalHelper(manager)
            scroller = Scroller(recyclerView.context, EASE_OUT)
            measurePage(manager)
        } else { helper = null; scroller = null }
        super.attachToRecyclerView(recyclerView)
    }

    private fun measurePage(manager: RecyclerView.LayoutManager) {
        if (unit != 0) return
        val first = manager.getChildAt(0) ?: return
        val view = recycler ?: return
        unit = if (manager.canScrollHorizontally()) first.width else first.height
        if (unit <= 0) { unit = 0; return }
        val spans = (manager as? GridLayoutManager)?.spanCount ?: 1
        pageItems = spans * (if (manager.canScrollHorizontally()) view.width / unit else view.height / unit)
    }

    override fun calculateDistanceToFinalSnap(layoutManager: RecyclerView.LayoutManager, targetView: View): IntArray {
        val helper = helper ?: return intArrayOf(0, 0)
        val distance = if (rtl) helper.getDecoratedEnd(targetView) - (recycler?.width ?: 0) else helper.getDecoratedStart(targetView)
        val result = if (layoutManager.canScrollHorizontally()) intArrayOf(distance, 0) else intArrayOf(0, distance)
        changed(layoutManager.getPosition(targetView))
        return result
    }

    override fun findSnapView(layoutManager: RecyclerView.LayoutManager): View? {
        val manager = layoutManager as? LinearLayoutManager ?: return null
        val first = manager.findFirstVisibleItemPosition()
        if (first == RecyclerView.NO_POSITION) return null
        measurePage(manager)
        if (pageItems <= 0) return null
        val target: Int
        if (first >= previousFirst) {
            val complete = manager.findFirstCompletelyVisibleItemPosition()
            target = if (complete == RecyclerView.NO_POSITION || complete % pageItems != 0) floorPage(pageItems + first) else complete
        } else {
            target = floorPage(first)
            if (manager.findViewByPosition(target) == null) {
                val delta = distanceToMissing(manager, target)
                recycler?.smoothScrollBy(delta[0], delta[1], EASE_OUT)
            }
        }
        previousFirst = first
        return manager.findViewByPosition(target)
    }

    private fun distanceToMissing(manager: LinearLayoutManager, target: Int): IntArray {
        val result = intArrayOf(0, 0)
        val first = manager.findFirstVisibleItemPosition()
        val helper = helper ?: return result
        if (target <= first && manager.canScrollHorizontally()) {
            val view = manager.findViewByPosition(if (rtl) manager.findLastVisibleItemPosition() else first) ?: return result
            result[0] = if (rtl) helper.getDecoratedEnd(view) + (first - target) * unit else helper.getDecoratedStart(view) - (first - target) * unit
        }
        if (target <= first && manager.canScrollVertically()) result[1] = (manager.findViewByPosition(first)?.top ?: 0) - (first - target) * unit
        return result
    }

    override fun findTargetSnapPosition(layoutManager: RecyclerView.LayoutManager, velocityX: Int, velocityY: Int): Int {
        val manager = layoutManager as? LinearLayoutManager ?: return RecyclerView.NO_POSITION
        measurePage(manager)
        if (pageItems <= 0 || unit <= 0) return RecyclerView.NO_POSITION
        val scroller = scroller ?: return RecyclerView.NO_POSITION
        scroller.fling(0, 0, velocityX, velocityY, Int.MIN_VALUE, Int.MAX_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
        val distance = when { velocityX != 0 -> scroller.finalX; velocityY != 0 -> scroller.finalY; else -> return RecyclerView.NO_POSITION }
        // i8.a(1) clamps a fling to one page after rounding its item count upward.
        var items = floorPage(abs(distance) / unit + pageItems - 1).coerceIn(pageItems, pageItems)
        if (distance < 0) items = -items
        if (rtl) items = -items
        val firstSide = if (rtl) distance < 0 else distance >= 0
        val base = if (firstSide) manager.findFirstVisibleItemPosition() else manager.findLastVisibleItemPosition()
        return floorPage(base) + items
    }

    override fun createScroller(layoutManager: RecyclerView.LayoutManager): RecyclerView.SmoothScroller? {
        val recycler = recycler ?: return null
        if (layoutManager !is RecyclerView.SmoothScroller.ScrollVectorProvider) return null
        return object : LinearSmoothScroller(recycler.context) {
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics) = 50f / displayMetrics.densityDpi
            override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
                val delta = calculateDistanceToFinalSnap(layoutManager, targetView)
                val time = calculateTimeForDeceleration(max(abs(delta[0]), abs(delta[1])))
                if (time > 0) action.update(delta[0], delta[1], time, EASE_OUT)
            }
        }
    }
    private fun floorPage(position: Int) = position - position % pageItems
    private companion object {
        val EASE_OUT = Interpolator { input -> val value = input - 1f; value * value * value + 1f }
    }
}
