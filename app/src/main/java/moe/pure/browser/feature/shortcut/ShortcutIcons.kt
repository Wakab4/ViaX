package moe.pure.browser.feature.shortcut

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import moe.pure.browser.ui.components.dp
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/** All custom images are decoded to a bounded bitmap; saved state only contains a cache token. */
class ShortcutIcons(private val context: Context) {
    private val directory = File(context.cacheDir, "shortcut-icons")
    fun import(uri: Uri): String {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val largest = maxOf(info.size.width, info.size.height)
            decoder.setTargetSize((info.size.width * 144L / largest).toInt().coerceAtLeast(1), (info.size.height * 144L / largest).toInt().coerceAtLeast(1))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        try { return save(bitmap) } finally { bitmap.recycle() }
    }
    fun save(bitmap: Bitmap): String {
        check(directory.mkdirs() || directory.isDirectory)
        val token = UUID.randomUUID().toString()
        val largest = maxOf(bitmap.width, bitmap.height)
        val bounded = if (largest <= 144) bitmap else Bitmap.createScaledBitmap(bitmap, (bitmap.width * 144L / largest).toInt().coerceAtLeast(1), (bitmap.height * 144L / largest).toInt().coerceAtLeast(1), true)
        try { file(token)!!.outputStream().use { check(bounded.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
        catch (error: Exception) { file(token)?.delete(); throw error }
        finally { if (bounded !== bitmap) bounded.recycle() }
        return token
    }
    fun read(token: String?): Bitmap? = file(token)?.takeIf(File::isFile)?.let { BitmapFactory.decodeFile(it.absolutePath) }
    fun remove(token: String?) { file(token)?.delete() }
    private fun file(token: String?): File? = token?.takeIf { TOKEN.matches(it) }?.let { File(directory, "$it.png") }
    fun fallback(url: String, title: String): Bitmap {
        val size = context.dp(108)
        val digest = MessageDigest.getInstance("MD5").digest(url.ifEmpty { "http://viayoo.com/" }.toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
        val color = Color.rgb(digest.take(3).toInt(16) % 128 + 100, digest.takeLast(3).toInt(16) % 128 + 100, digest.substring(13, 16).toInt(16) % 128 + 100)
        val label = title.ifBlank { Uri.parse(url).host.orEmpty().removePrefix("www.") }.let { if (it.isEmpty()) "" else String(Character.toChars(it.codePointAt(0))) }
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            eraseColor(color)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = Color.WHITE; textSize = maxOf(context.dp(16), size / 4).toFloat() }
            val bounds = Rect(); paint.getTextBounds(label, 0, label.length, bounds)
            Canvas(this).drawText(label, ((size - bounds.width()) / 2).toFloat(), ((size + bounds.height()) / 2).toFloat(), paint)
        }
    }
    fun padded(source: Bitmap): Bitmap {
        val paddingX = (source.width * .32f).toInt()
        val paddingY = (source.height * .32f).toInt()
        return Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(source.getPixel(0, 0))
            val left = paddingX / 2; val top = paddingY / 2
            Canvas(this).drawBitmap(source, null, RectF(left.toFloat(), top.toFloat(), (left + source.width - paddingX).toFloat(), (top + source.height - paddingY).toFloat()), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
    }
    companion object { private val TOKEN = Regex("[0-9a-fA-F-]{36}") }
}
