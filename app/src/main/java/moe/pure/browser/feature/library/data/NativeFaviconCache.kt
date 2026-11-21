package moe.pure.browser.feature.library.data

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache

/** GP x0: one process-wide KiB budget, successful images only, keyed by their complete file paths. */
internal object NativeFaviconCache {
    private val images = object : LruCache<String, Bitmap>(maxOf(2048, (Runtime.getRuntime().maxMemory() / 1024).toInt() / 64)) {
        override fun sizeOf(key: String, value: Bitmap): Int = if (value.isRecycled) 0 else maxOf(1, value.byteCount / 1024)
    }

    fun bitmap(context: Context, source: String): Bitmap? {
        val file = runCatching { NativeFaviconPaths.file(context, source) }.getOrNull() ?: return null
        images[file.path]?.takeUnless(Bitmap::isRecycled)?.let { return it }
        val bitmap = NativeFaviconPaths.bitmap(context, file)
        if (bitmap != null && !bitmap.isRecycled) images.put(file.path, bitmap)
        return bitmap
    }

    /** Views may still display these bitmaps; Native does not recycle them when removing cache entries. */
    fun evictAll() = images.evictAll()
}
