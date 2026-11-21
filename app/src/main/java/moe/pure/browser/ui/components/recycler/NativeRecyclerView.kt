package moe.pure.browser.ui.components.recycler

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import moe.pure.browser.ui.components.scroll.NativeViewFastScroller

/** u0 delegates scroll metrics, drawing and thumb input while ordinary input stays in RecyclerView. */
open class NativeRecyclerView(context: Context) : RecyclerView(context) {
    private var fastScroller: NativeViewFastScroller? = null

    init {
        isVerticalScrollBarEnabled = false
        fastScroller = NativeViewFastScroller(this, object : NativeViewFastScroller.Surface {
            override fun scrollRange() = computeVerticalScrollRange()
            override fun scrollExtent() = computeVerticalScrollExtent()
            override fun scrollOffset() = computeVerticalScrollOffset()
            override fun ordinaryTouch(event: MotionEvent) = recyclerTouch(event)
        })
    }

    private fun recyclerTouch(event: MotionEvent) = super.onTouchEvent(event)
    override fun awakenScrollBars() = fastScroller?.awaken() ?: false
    override fun dispatchDraw(canvas: Canvas) { super.dispatchDraw(canvas); fastScroller?.draw(canvas) }
    override fun onInterceptTouchEvent(event: MotionEvent) = fastScroller?.intercept(event) == true || super.onInterceptTouchEvent(event)
    override fun onTouchEvent(event: MotionEvent) = fastScroller?.touch(event) == true || super.onTouchEvent(event)

    protected fun cancelFastScroll() { fastScroller?.cancel() }
    fun dispose() {
        stopScroll()
        itemAnimator?.endAnimations()
        (edgeEffectFactory as? NativeRecyclerEdgeEffect)?.close()
        cancelFastScroll()
    }
}
