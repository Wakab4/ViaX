package moe.via.browser.feature.appearance

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

/** Native s4.b.e draws the existing WebView into an unscaled 1 × 1 canvas and reads its top-left pixel. */
internal object PageColorSampler {
    fun sample(view: View): Int {
        var image: Bitmap? = null
        return try {
            image = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(image))
            image.getPixel(0, 0)
        } catch (_: RuntimeException) { 0 }
        finally { image?.recycle() }
    }
}
