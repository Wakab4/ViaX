package moe.via.browser.browser.ui.chrome

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * Keeps the expanded WebView measured at the window height while toolbars move over it.
 * Popovers retain a separate area between the currently visible top and bottom bars.
 */
internal class ChromeViewportLayout(context: Context) : ViewGroup(context) {
    val topBars = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    val bottomBars = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    val floating = FrameLayout(context)
    private var page: FrameLayout? = null
    private var overlays: FrameLayout? = null
    private var pageCover: FrameLayout? = null
    private var progress: View? = null
    private var search: View? = null
    private var passwords: View? = null
    private var backdrop: View? = null
    private var externalOverlayEdge: Int? = null
    private val decorations = mutableSetOf<View>()
    private var bottomSuppressed = false
    var expanded = false
        private set
    var barsInLayout = true
        private set
    var hiddenFraction = 0f
        private set
    val topHeight get() = topBars.measuredHeight
    val bottomHeight get() = bottomBars.measuredHeight

    init { clipChildren = true; clipToPadding = true }

    fun backdrop(view: View) { backdrop = view; addView(view, 0); requestLayout() }
    fun externalOverlay(edge: Int?) { externalOverlayEdge = edge; applyMotion() }

    fun install(page: FrameLayout, overlays: FrameLayout, progress: View, search: View, passwords: View, cover: FrameLayout) {
        this.page = page; this.overlays = overlays; this.progress = progress
        pageCover = cover
        this.search = search; this.passwords = passwords
        listOf(page, progress, cover, overlays, topBars, bottomBars, search, passwords, floating).forEach { child ->
            (child.parent as? ViewGroup)?.removeView(child)
            addView(child)
        }
    }

    fun setExpanded(value: Boolean) {
        if (expanded == value) return
        expanded = value
        requestLayout()
        applyMotion()
    }

    fun setBarsInLayout(value: Boolean) {
        barsInLayout = value
        topBars.visibility = if (value) View.VISIBLE else View.GONE
        bottomBars.visibility = if (value && !bottomSuppressed) View.VISIBLE else View.GONE
        requestLayout()
        applyMotion()
    }

    fun keyboard(visible: Boolean) {
        if (bottomSuppressed == visible) return
        bottomSuppressed = visible
        bottomBars.visibility = if (barsInLayout && !visible) View.VISIBLE else View.GONE
        requestLayout()
        applyMotion()
    }

    fun moveTo(fraction: Float) {
        hiddenFraction = fraction.coerceIn(0f, 1f)
        applyMotion()
    }

    fun decoration(view: View?, old: View? = null) {
        if (old != null) decorations.remove(old)
        if (view != null) decorations.add(view)
    }

    fun addToolbar(view: View, top: Boolean) {
        (view.parent as? ViewGroup)?.removeView(view)
        (if (top) topBars else bottomBars).addView(view, LinearLayout.LayoutParams(-1, view.layoutParams?.height ?: -2))
        requestLayout()
    }

    fun tabStrip(view: View?, top: Boolean, height: Int) {
        if (view == null) return
        (view.parent as? ViewGroup)?.removeView(view)
        val column = if (top) topBars else bottomBars
        column.addView(view, if (top) column.childCount else 0, LinearLayout.LayoutParams(-1, height))
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = resolveSize(suggestedMinimumHeight, heightMeasureSpec)
        setMeasuredDimension(width, height)
        backdrop?.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
        val availableWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        val availableHeight = (height - paddingTop - paddingBottom).coerceAtLeast(0)
        val exactWidth = MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY)
        val barLimit = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        // Measure even a hidden column, so show animations and orientation changes use current heights.
        topBars.measure(exactWidth, barLimit)
        bottomBars.measure(exactWidth, barLimit)
        val top = if (barsInLayout) topHeight else 0
        val bottom = if (barsInLayout && !bottomSuppressed) bottomHeight else 0
        val between = (availableHeight - top - bottom).coerceAtLeast(0)
        page?.measure(exactWidth, MeasureSpec.makeMeasureSpec(if (expanded) availableHeight else between, MeasureSpec.EXACTLY))
        pageCover?.measure(exactWidth, MeasureSpec.makeMeasureSpec(if (expanded) availableHeight else between, MeasureSpec.EXACTLY))
        overlays?.measure(exactWidth, MeasureSpec.makeMeasureSpec(between, MeasureSpec.EXACTLY))
        floating.measure(exactWidth, MeasureSpec.makeMeasureSpec(availableHeight, MeasureSpec.EXACTLY))
        listOfNotNull(progress, search, passwords).forEach { child ->
            val requested = child.layoutParams?.height ?: 0
            child.measure(exactWidth, MeasureSpec.makeMeasureSpec(requested.coerceAtLeast(0), MeasureSpec.EXACTLY))
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val x = paddingLeft
        backdrop?.layout(0, 0, width, height)
        val y = paddingTop
        val edge = height - paddingBottom
        val visibleTop = if (barsInLayout) topHeight else 0
        val visibleBottom = if (barsInLayout && !bottomSuppressed) bottomHeight else 0
        fun place(view: View?, at: Int) {
            if (view != null) view.layout(x, at, x + view.measuredWidth, at + view.measuredHeight)
        }
        place(topBars, y)
        place(bottomBars, edge - bottomHeight)
        place(page, if (expanded) y else y + visibleTop)
        place(pageCover, if (expanded) y else y + visibleTop)
        place(overlays, y + visibleTop)
        place(floating, y)
        progress?.let { place(it, if (topBars.childCount > 0) y + visibleTop else edge - visibleBottom - it.measuredHeight) }
        search?.let { place(it, edge - visibleBottom - it.measuredHeight) }
        passwords?.let { place(it, edge - visibleBottom - it.measuredHeight) }
        applyMotion()
    }

    private fun applyMotion() {
        val top = topHeight.toFloat()
        val bottom = if (bottomSuppressed) 0f else bottomHeight.toFloat()
        topBars.translationY = -top * hiddenFraction
        bottomBars.translationY = bottom * hiddenFraction
        page?.translationY = if (expanded) top * (1f - hiddenFraction) else 0f
        pageCover?.translationY = page?.translationY ?: 0f
        val fraction = if (barsInLayout) hiddenFraction else 0f
        progress?.translationY = (if (topBars.childCount > 0) -top else bottom) * fraction
        search?.translationY = bottom * fraction
        passwords?.translationY = bottom * fraction
        overlays?.translationY = when (overlayEdge()) {
            Gravity.TOP -> -top * fraction
            Gravity.BOTTOM -> bottom * fraction
            else -> 0f
        }
    }

    /** Mirrors ha(): only a single popover with an explicitly anchored first child follows chrome. */
    private fun overlayEdge(): Int? {
        externalOverlayEdge?.let { return it }
        val host = overlays ?: return null
        val children = (0 until host.childCount).map(host::getChildAt)
            .filter { it !in decorations && it.visibility == View.VISIBLE }
        val layer = children.singleOrNull() as? ViewGroup ?: return null
        val params = layer.getChildAt(0)?.layoutParams as? FrameLayout.LayoutParams ?: return null
        return if (params.gravity and Gravity.BOTTOM == Gravity.BOTTOM) Gravity.BOTTOM else Gravity.TOP
    }
}
