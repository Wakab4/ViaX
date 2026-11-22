package moe.via.browser.feature.pdf.ui

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import moe.via.browser.ui.components.dp
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.floor
import kotlin.math.roundToInt

/** Touch, pinch and inertia operate the same transform; raster scheduling stays with the view. */
internal class PdfGestures(private val view: View, private val viewport: PdfViewport,
    private val changed: () -> Unit, private val settled: () -> Unit, private val zooming: () -> Unit) : AutoCloseable {
    var mode = 0
        private set
    private val edge = PdfEdgeStretch(view)
    private val scroller = OverScroller(view.context)
    private val minimumVelocity = ViewConfiguration.get(view.context).scaledMinimumFlingVelocity
    private var flinging = false
    private var absorbed = false
    private var distance = 1f
    private var startZoom = 1f
    private var startX = 0f
    private var startY = 0f
    private var anchorX = 0f
    private var anchorY = 0f
    private val detector = GestureDetector(view.context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(event: MotionEvent): Boolean { view.performClick(); return true }
        override fun onScroll(first: MotionEvent?, event: MotionEvent, dx: Float, dy: Float): Boolean {
            val wantedY = viewport.y - dy
            val clampedY = viewport.clampY(wantedY)
            val overflow = wantedY - clampedY
            val pull = abs(dy) >= abs(dx) && overflow != 0f && clampedY == viewport.y
            if (pull) edge.pull(overflow) else edge.release()
            if (viewport.move(viewport.x - if (pull) 0f else dx, clampedY)) changed()
            return pull
        }
        override fun onFling(first: MotionEvent?, event: MotionEvent, vx: Float, vy: Float): Boolean {
            if (first == null || abs(first.x - event.x) < 100f && abs(first.y - event.y) < 100f ||
                abs(vx) < minimumVelocity && abs(vy) < minimumVelocity) return false
            if (viewport.y >= 0f && vy > 0f || viewport.y <= viewport.minY && vy < 0f) edge.absorb(vy)
            else {
                val centered = viewport.width * viewport.zoom <= viewport.width
                val minimumX = if (centered) viewport.clampX(viewport.x).roundToInt() else floor(viewport.minX.toDouble()).toInt()
                val maximumX = if (centered) minimumX else 0
                scroller.fling(viewport.x.roundToInt(), viewport.y.roundToInt(), vx.roundToInt(), vy.roundToInt(),
                    minimumX, maximumX, floor(viewport.minY.toDouble()).toInt(), 0)
                flinging = !scroller.isFinished; absorbed = false
                if (flinging) view.postInvalidateOnAnimation() else settled()
            }
            return true
        }
    })

    fun touch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { mode = 1; abort(); detector.onTouchEvent(event); return true }
            MotionEvent.ACTION_POINTER_DOWN -> {
                mode = 2; abort()
                if (event.pointerCount >= 2) {
                    distance = separation(event).coerceAtLeast(.001f); startZoom = viewport.zoom
                    startX = viewport.x; startY = viewport.y
                    anchorX = (event.getX(0) + event.getX(1)) / 2; anchorY = (event.getY(0) + event.getY(1)) / 2
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == 1) { detector.onTouchEvent(event); return true }
                if (mode == 2 && event.pointerCount >= 2) {
                    zooming()
                    viewport.scale(startZoom * separation(event) / distance, anchorX, anchorY, startX, startY, startZoom,
                        (event.getX(0) + event.getX(1)) / 2, (event.getY(0) + event.getY(1)) / 2, view.dp(24).toFloat())
                    changed(); return true
                }
            }
            MotionEvent.ACTION_UP -> {
                if (mode == 1 && !detector.onTouchEvent(event)) { edge.release(); settled() }
                else if (mode == 2) settled()
                mode = 0; return true
            }
            MotionEvent.ACTION_CANCEL -> { edge.release(); mode = 0; return true }
        }
        return mode != 0
    }

    fun compute() {
        if (!scroller.computeScrollOffset()) {
            if (flinging) { flinging = false; absorbed = false; settled() }
            return
        }
        val oldY = viewport.y
        if (viewport.move(scroller.currX.toFloat(), scroller.currY.toFloat())) changed()
        val top = oldY < 0f && viewport.y >= 0f
        val bottom = oldY > viewport.minY && viewport.y <= viewport.minY
        if (flinging && !absorbed && (top || bottom)) {
            absorbed = true; edge.absorb(scroller.currVelocity * if (top) 1f else -1f)
        }
        view.postInvalidateOnAnimation()
    }
    fun abort() { scroller.abortAnimation(); flinging = false; absorbed = false }
    fun releaseEdge() = edge.release()
    private fun separation(event: MotionEvent) = hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1))
    override fun close() { abort(); mode = 0; edge.close() }
}
