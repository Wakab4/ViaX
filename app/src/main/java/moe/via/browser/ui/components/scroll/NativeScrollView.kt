package moe.via.browser.ui.components.scroll

import android.content.Context
import android.graphics.Canvas
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView

/** q4.b uses the shared thumb engine over ScrollView metrics; page owners choose their content. */
open class NativeScrollView(context: Context) : ScrollView(context) {
    private var fastScroller: NativeViewFastScroller? = null
    init {
        isFillViewport = true; isVerticalScrollBarEnabled = false
        overScrollMode = if (Build.VERSION.SDK_INT >= 32) View.OVER_SCROLL_IF_CONTENT_SCROLLS else View.OVER_SCROLL_NEVER
        fastScroller = NativeViewFastScroller(this, object : NativeViewFastScroller.Surface {
            override fun scrollRange() = computeVerticalScrollRange()
            override fun scrollExtent() = computeVerticalScrollExtent()
            override fun scrollOffset() = computeVerticalScrollOffset()
            override fun ordinaryTouch(event: MotionEvent) = scrollTouch(event)
        })
    }
    private fun scrollTouch(event: MotionEvent) = super.onTouchEvent(event)
    override fun awakenScrollBars() = fastScroller?.awaken() ?: false
    override fun dispatchDraw(canvas: Canvas) { super.dispatchDraw(canvas); fastScroller?.draw(canvas) }
    override fun onInterceptTouchEvent(event: MotionEvent) = fastScroller?.intercept(event) == true || super.onInterceptTouchEvent(event)
    override fun onTouchEvent(event: MotionEvent) = fastScroller?.touch(event) == true || super.onTouchEvent(event)
    fun dispose() { fastScroller?.cancel(); fastScroller = null }
}
