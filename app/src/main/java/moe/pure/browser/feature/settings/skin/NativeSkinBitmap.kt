package moe.pure.browser.feature.settings.skin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import java.io.File

/** lb.a.b/z8.i: decode bounds, power-of-two sample, centered crop and downscale to 96px. */
internal object NativeSkinBitmap {
    fun load(path: String?): Drawable? {
        if (path.isNullOrEmpty() || !File(path).exists()) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        var sample = 1
        if (options.outWidth > 0 && options.outHeight > 0 && (options.outHeight > SIZE || options.outWidth > SIZE)) {
            while (options.outHeight / 2 / sample >= SIZE && options.outWidth / 2 / sample >= SIZE) sample *= 2
        }
        options.inSampleSize = sample; options.inJustDecodeBounds = false
        val decoded = BitmapFactory.decodeFile(path, options) ?: return null
        if (decoded.isRecycled || decoded.width <= 0 || decoded.height <= 0) return null
        val edge = minOf(decoded.width, decoded.height)
        val cropped = Bitmap.createBitmap(decoded, (decoded.width - edge) / 2, (decoded.height - edge) / 2, edge, edge)
        val result = if (edge > SIZE) Bitmap.createScaledBitmap(cropped, SIZE, SIZE, true) else cropped
        @Suppress("DEPRECATION")
        return BitmapDrawable(null, result)
    }

    private const val SIZE = 96
}
