package moe.pure.browser.feature.scan.ui

import android.content.Context
import android.view.SurfaceView
import android.widget.FrameLayout
import moe.pure.browser.feature.scan.camera.ScanPreviewInfo
import kotlin.math.max
import kotlin.math.roundToInt

/** Native fb.q selects SurfaceView with u5.e's centered cover scaling. */
internal class ScannerPreview(context: Context) : FrameLayout(context) {
    val surface = SurfaceView(context)
    private var previewWidth = 0
    private var previewHeight = 0
    init { clipChildren = true; addView(surface, LayoutParams(-1, -1)) }
    fun preview(info: ScanPreviewInfo) {
        previewWidth = if (info.rotation % 180 == 0) info.width else info.height
        previewHeight = if (info.rotation % 180 == 0) info.height else info.width
        requestLayout()
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val scale = if (previewWidth > 0 && previewHeight > 0) max(measuredWidth.toFloat() / previewWidth, measuredHeight.toFloat() / previewHeight) else 1f
        val width = if (previewWidth > 0) (previewWidth * scale).roundToInt() else measuredWidth
        val height = if (previewHeight > 0) (previewHeight * scale).roundToInt() else measuredHeight
        surface.measure(MeasureSpec.makeMeasureSpec(width.coerceAtLeast(1), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height.coerceAtLeast(1), MeasureSpec.EXACTLY))
    }
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val x = (width - surface.measuredWidth) / 2
        val y = (height - surface.measuredHeight) / 2
        surface.layout(x, y, x + surface.measuredWidth, y + surface.measuredHeight)
    }
}
