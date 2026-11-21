package moe.pure.browser.feature.library.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import moe.pure.browser.data.BrowserExecutors
import kotlin.math.roundToInt

/** Native y0 persistence and the two explicit global cache-clear entry points. */
object BookmarkFaviconStore {
    /** Enqueues PNG persistence; neither the borrowed WebView bitmap nor the process cache is replaced. */
    fun save(context: Context, url: String?, bitmap: Bitmap?) {
        if (url.isNullOrEmpty() || bitmap == null || bitmap.isRecycled) return
        val app = context.applicationContext
        BrowserExecutors.persistence.execute {
            val key = authority(url) ?: return@execute
            runCatching {
                val destination = NativeFaviconPaths.forAuthority(app, key) ?: return@runCatching
                val image = rounded(bitmap, dp(app, 24), dp(app, 4)) ?: return@runCatching
                try {
                    if (destination.exists() && !destination.delete()) return@runCatching
                    destination.outputStream().use { output -> image.compress(Bitmap.CompressFormat.PNG, 100, output); output.flush() }
                } finally { image.recycle() }
            }
        }
    }

    /** y0.b retains original case and its unusual colon handling for schemeless names. */
    fun authority(url: String): String? = NativeFaviconPaths.authority(url)

    /** Called on the persistence worker only for Native global history-data clearing, not range/row deletion. */
    fun clearHistoryAssets(context: Context) = FaviconHistoryAssets.clear(context.applicationContext)

    /** App-cache clearing discards memory entries but preserves favicon files. Closing or recoloring UI does not. */
    fun evictMemory() = NativeFaviconCache.evictAll()

    private fun rounded(source: Bitmap, maximum: Int, radius: Int): Bitmap? {
        if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
        val side = minOf(source.width, source.height)
        val scale = minOf(maximum.toFloat() / side, 1f)
        val edge = if (scale < 1f) maximum else side
        val scaled = if (scale < 1f) Bitmap.createBitmap(source, 0, 0, source.width, source.height,
            Matrix().apply { postScale(scale, scale) }, true) else source
        try {
            return Bitmap.createBitmap(edge, edge, Bitmap.Config.ARGB_8888).also { result ->
                val canvas = Canvas(result)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                canvas.drawRoundRect(RectF(0f, 0f, edge.toFloat(), edge.toFloat()), radius.toFloat(), radius.toFloat(), paint)
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                canvas.drawBitmap(scaled, edge / 2f - source.width * scale / 2f, edge / 2f - source.height * scale / 2f, paint)
            }
        } finally { if (scaled !== source) scaled.recycle() }
    }

    private fun dp(context: Context, value: Int) = (context.resources.displayMetrics.density * value).roundToInt().coerceAtLeast(1)
}
