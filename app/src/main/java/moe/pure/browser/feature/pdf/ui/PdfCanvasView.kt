package moe.pure.browser.feature.pdf.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import moe.pure.browser.feature.pdf.render.PdfDrawingPage
import moe.pure.browser.feature.pdf.render.PdfPageSize
import moe.pure.browser.feature.pdf.render.PdfRenderController
import moe.pure.browser.ui.components.scroll.NativeViewFastScroller
import kotlin.math.abs
import kotlin.math.roundToInt

/** Continuous white pages on the original grey canvas, with base and visible high-resolution layers. */
internal class PdfCanvasView(context: Context) : View(context), AutoCloseable {
    private val viewport = PdfViewport((8f * resources.displayMetrics.density).toInt().toFloat())
    private var sizes = emptyList<PdfPageSize>()
    private var base = emptyList<PdfDrawingPage>()
    private var high = emptyList<PdfDrawingPage>()
    private var current = 0
    private var disposed = false
    private var downX = 0f
    private var downY = 0f
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val paper = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = -1 }
    var pageChanged: ((Int, Int) -> Unit)? = null
    val zoom get() = viewport.zoom
    private val listener = object : PdfRenderController.Listener {
        override fun loaded(sizes: List<PdfPageSize>) {
            if (disposed) return
            this@PdfCanvasView.sizes = sizes
            layoutPages(reset = true)
        }
        override fun baseReady(current: Int, pages: List<PdfDrawingPage>) {
            if (disposed || current != this@PdfCanvasView.current || pages.isEmpty()) return
            base = pages; invalidate()
            if (gestures.mode == 0) requestHigh()
        }
        override fun highReady(current: Int, pages: List<PdfDrawingPage>) {
            if (disposed || current != this@PdfCanvasView.current || pages.isEmpty()) return
            high = pages; invalidate()
        }
    }
    private val renderer = PdfRenderController(context, listener)
    private val gestures = PdfGestures(this, viewport, ::moved, ::requestBase, ::clearHigh)
    private val fastScroller = NativeViewFastScroller(this, object : NativeViewFastScroller.Surface {
        override fun scrollRange() = viewport.range
        override fun scrollExtent() = height
        override fun scrollOffset() = viewport.offset
        override fun ordinaryTouch(event: MotionEvent) = gestures.touch(event)
    }, pdfStyle = true)

    init { setBackgroundColor(0x80cccccc.toInt()); isClickable = true; isFocusable = true }

    fun open(uri: Uri) {
        if (disposed) return
        sizes = emptyList(); base = emptyList(); high = emptyList(); current = 0
        gestures.close(); renderer.open(uri); layoutPages(reset = true)
    }
    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        gestures.abort(); layoutPages(reset = false)
    }
    private fun layoutPages(reset: Boolean) {
        if (width <= 0) return
        clearHigh(); base = emptyList()
        viewport.layout(sizes, width, height, reset)
        current = viewport.current(viewport.visible(), current).coerceIn(0, maxOf(0, sizes.lastIndex))
        if (sizes.isNotEmpty()) { pageChanged?.invoke(current, sizes.size); fastScroller.awaken() }
        requestBase(); invalidate()
    }
    private fun moved() {
        val visible = viewport.visible()
        val previous = current
        current = viewport.current(visible, current)
        if (previous != current) {
            pageChanged?.invoke(current, sizes.size)
            // Native N only schedules at a multi-page boundary; the single-page branch reports its counter.
            if (visible.first != visible.last) requestBase()
        }
        if (!visible.isEmpty()) high = high.filter { it.page.index in (visible.first - 1)..(visible.last + 1) }
        fastScroller.awaken(); invalidate()
    }
    private fun requestBase() { if (!disposed && viewport.pages.isNotEmpty()) renderer.base(viewport.pages, current) }
    private fun requestHigh() {
        val visible = viewport.visible()
        if (!disposed && !visible.isEmpty()) renderer.high(viewport.pages, current, viewport.zoom, visible)
    }
    private fun clearHigh() {
        if (high.isEmpty()) return
        renderer.cancelHigh(); high = emptyList(); invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (viewport.pages.isNotEmpty()) {
            canvas.drawColor(0xff666666.toInt())
            val checkpoint = canvas.save()
            canvas.translate(viewport.x, viewport.y); canvas.scale(viewport.zoom, viewport.zoom)
            viewport.pages.forEach { canvas.drawRect(it.bounds, paper) }
            val highIndices = high.map { it.page.index }.toSet()
            base.forEach { if (it.page.index !in highIndices) drawPage(canvas, it) }
            high.forEach { drawPage(canvas, it) }
            canvas.restoreToCount(checkpoint)
        }
        fastScroller.draw(canvas)
    }
    private fun drawPage(canvas: Canvas, page: PdfDrawingPage) {
        if (page.bitmap.isRecycled) return
        canvas.drawBitmap(page.bitmap, null, page.page.bounds, bitmapPaint)
    }
    override fun computeScroll() { super.computeScroll(); gestures.compute() }
    override fun scrollTo(x: Int, y: Int) {
        gestures.abort(); gestures.releaseEdge()
        if (viewport.move(-x.toFloat(), -y.toFloat())) moved()
    }
    override fun scrollBy(x: Int, y: Int) = scrollTo(maxOf(0, (-viewport.x).roundToInt()) + x, viewport.offset + y)
    override fun computeVerticalScrollRange() = viewport.range
    override fun computeVerticalScrollExtent() = height
    override fun computeVerticalScrollOffset() = viewport.offset

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (viewport.zoom >= 1f) {
            if (gestures.mode == 2 || viewport.x != 0f) parent?.requestDisallowInterceptTouchEvent(true)
            else when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { downX = event.x; downY = event.y; parent?.requestDisallowInterceptTouchEvent(true) }
                MotionEvent.ACTION_MOVE -> if (abs(event.x - downX) > 10f && abs(event.x - downX) > abs(event.y - downY))
                    parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.dispatchTouchEvent(event)
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = fastScroller.touch(event) || gestures.touch(event) || super.onTouchEvent(event)
    override fun performClick(): Boolean { super.performClick(); return true }
    override fun onDetachedFromWindow() { fastScroller.cancel(); gestures.close(); super.onDetachedFromWindow() }
    override fun close() {
        if (disposed) return
        disposed = true; pageChanged = null; fastScroller.cancel(); gestures.close(); renderer.close()
        sizes = emptyList(); base = emptyList(); high = emptyList()
    }
}
