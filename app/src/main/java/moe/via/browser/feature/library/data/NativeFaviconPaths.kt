package moe.via.browser.feature.library.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import java.io.File
import java.security.MessageDigest

/** y0's source parsing and file decoding; callers choose cached Bitmap or direct Drawable semantics. */
internal object NativeFaviconPaths {
    data class Direct(val exists: Boolean, val drawable: Drawable?)

    fun authority(source: String): String? {
        if (source.isEmpty()) return null
        val scheme = source.indexOf("://")
        val start = if (scheme >= 0) scheme + 3 else source.indexOf(':') + 1
        val end = source.indexOf('/', start).takeIf { it >= 0 }
            ?: source.indexOf('?', start).takeIf { it >= 0 } ?: source.length
        return source.substring(start, end).takeIf(String::isNotEmpty)
    }

    fun file(context: Context, source: String): File? = authority(source)?.let { forAuthority(context, it) }

    fun forAuthority(context: Context, authority: String): File? {
        val directory = directory(context, "favicons")
        if (!directory.exists() && !directory.mkdirs()) return null
        val digest = MessageDigest.getInstance("MD5").digest(authority.toByteArray(Charsets.UTF_8))
        val alphabet = "0123456789abcdef"
        val name = buildString(32) { digest.forEach { byte ->
            val number = byte.toInt() and 255
            append(alphabet[number ushr 4]); append(alphabet[number and 15])
        } }
        return File(directory, name)
    }

    fun directory(context: Context, name: String): File = context.getExternalFilesDir(name) ?: File(context.filesDir, name)

    /** a8.k and xa.b/c test existence before decoding, retaining an existing-but-invalid empty icon. */
    fun direct(context: Context, source: String): Direct {
        val file = runCatching { file(context, source) }.getOrNull() ?: return Direct(false, null)
        if (!file.exists()) return Direct(false, null)
        return Direct(true, runCatching { Drawable.createFromPath(file.path) }.getOrNull())
    }

    /** x0/y0.c cold reads are synchronous, downsampled then square-cropped to at most 24dp. */
    fun bitmap(context: Context, file: File): Bitmap? = runCatching {
        if (!file.exists()) return@runCatching null
        val edge = (24f * context.resources.displayMetrics.density + .5f).toInt().coerceAtLeast(1)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, options)
        var sample = 1
        while (options.outWidth / 2 / sample >= edge && options.outHeight / 2 / sample >= edge) sample *= 2
        options.inJustDecodeBounds = false; options.inSampleSize = sample
        val image = BitmapFactory.decodeFile(file.path, options) ?: return@runCatching null
        val side = minOf(image.width, image.height)
        val square = Bitmap.createBitmap(image, (image.width - side) / 2, (image.height - side) / 2, side, side)
        val result = if (side > edge) Bitmap.createScaledBitmap(square, edge, edge, true) else square
        if (square !== result && square !== image) square.recycle()
        if (image !== result) image.recycle()
        result
    }.getOrNull()
}
