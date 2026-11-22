package moe.via.browser.feature.home.background

import android.content.Context
import android.graphics.Color
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import moe.via.browser.core.graphics.NativeBitmapImages
import moe.via.browser.core.concurrent.BrowserExecutors
import moe.via.browser.feature.home.model.HomepageConfig
import java.io.File

/** Capture and bind on the view owner; decode and JPEG preparation use independent IO jobs. */
class HomeBackgroundLoader(private val context: Context) {
    data class Request(val source: String, val cache: File, val cached: Boolean, val width: Int, val height: Int)

    fun capture(config: HomepageConfig, width: Int, height: Int): Request? {
        val path = config.backgroundPath
        if (path.isNullOrEmpty() || !File(path).exists()) return null
        val cache = NativeBackgroundFiles.cacheFile(context)
        return Request(path, cache, cache.exists(), width, height)
    }

    fun image(request: Request): Drawable? {
        val bitmap = if (request.cached) BitmapFactory.decodeFile(request.cache.path)
            else NativeBitmapImages.decodeCrop(request.source, request.width, request.height)
        return bitmap?.let { NativeBitmapDrawable(context.resources, it) }
    }

    fun prepare(request: Request) {
        if (!request.cached) BrowserExecutors.io.execute { NativeBackgroundFiles.prepare(context.applicationContext, request.source) }
    }

    fun surface(config: HomepageConfig, image: Drawable?): Drawable {
        val baseColor = if (config.accentColor == -1) if (config.isNight) Color.BLACK else Color.WHITE else config.accentColor
        if (image == null) {
            // yb(0) / f8.k put an opaque surface above the image/filter layers when no image exists.
            val surface = if (config.isNight && config.accentColor != -1) Color.rgb(
                Color.red(baseColor) / 2, Color.green(baseColor) / 2, Color.blue(baseColor) / 2) else baseColor
            return ColorDrawable(surface)
        }
        // Native uses the stored accent (-1 is white) for the night baseline, before resolving the default color.
        val night = if (!config.isNight) 0 else if (isLight(config.accentColor)) 128 else 64
        val opacity = (config.backgroundOpacity * 255f).toInt()
        return LayerDrawable(arrayOf(ColorDrawable(baseColor), image, ColorDrawable(Color.argb(maxOf(night, opacity), 0, 0, 0))))
    }

    private fun isLight(color: Int) = .299 * Color.red(color) + .587 * Color.green(color) + .114 * Color.blue(color) >= 192
}
