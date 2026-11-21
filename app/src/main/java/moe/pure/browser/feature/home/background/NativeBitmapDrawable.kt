package moe.pure.browser.feature.home.background

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable

/** o0 shares its bitmap through ConstantState; each drawable keeps its own matrix and paint. */
internal class NativeBitmapDrawable private constructor(private val imageState: State) : Drawable() {
    private val matrix = Matrix()
    private val paint = Paint(3)

    constructor(resources: Resources?, bitmap: Bitmap) : this(State(bitmap, resources?.displayMetrics?.densityDpi ?: 160))

    override fun draw(canvas: Canvas) {
        if (imageState.bitmap.isRecycled || bounds.isEmpty) return
        val saved = canvas.save()
        canvas.clipRect(bounds)
        canvas.drawBitmap(imageState.bitmap, matrix, paint)
        canvas.restoreToCount(saved)
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val width = imageState.bitmap.width
        val height = imageState.bitmap.height
        if (bounds.isEmpty || width <= 0 || height <= 0) { matrix.reset(); return }
        val scale = maxOf(bounds.width() / width.toFloat(), bounds.height() / height.toFloat())
        matrix.setScale(scale, scale)
        matrix.postTranslate(bounds.left + (bounds.width() - width * scale) / 2f,
            bounds.top + (bounds.height() - height * scale) / 2f)
    }

    override fun getConstantState(): ConstantState = imageState
    override fun getIntrinsicWidth() = imageState.bitmap.getScaledWidth(imageState.density)
    override fun getIntrinsicHeight() = imageState.bitmap.getScaledHeight(imageState.density)
    override fun setAlpha(alpha: Int) { if (paint.alpha != alpha) { paint.alpha = alpha; invalidateSelf() } }
    override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter = filter; invalidateSelf() }
    @Deprecated("Drawable opacity is no longer used")
    override fun getOpacity() = if (imageState.bitmap.hasAlpha() || paint.alpha < 255) PixelFormat.TRANSLUCENT else PixelFormat.OPAQUE

    private class State(val bitmap: Bitmap, val density: Int) : ConstantState() {
        override fun getChangingConfigurations() = 0
        override fun newDrawable(): Drawable = NativeBitmapDrawable(this)
        override fun newDrawable(resources: Resources?): Drawable = NativeBitmapDrawable(State(bitmap, resources?.displayMetrics?.densityDpi ?: 160))
    }
}
