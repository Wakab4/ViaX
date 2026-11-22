package moe.via.browser.feature.home.icons

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import moe.via.browser.core.logging.NativeLog
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/** w3.s/t scale before the white-square or rounded-mask pass and recycle that working bitmap. */
internal object NativeHomeIconBitmaps {
    private class Prepared(val bitmap: Bitmap, val width: Int, val height: Int, val left: Float, val top: Float)

    private fun prepare(input: Bitmap?, width: Int, height: Int): Prepared? {
        if (input == null || input.isRecycled) return null
        val sourceWidth = input.width
        val sourceHeight = input.height
        val ratio = width / height.toFloat()
        val cropWidth: Int
        val cropHeight: Int
        if (sourceWidth.toFloat() / sourceHeight >= ratio) { cropWidth = (ratio * sourceHeight).toInt(); cropHeight = sourceHeight }
        else { cropWidth = sourceWidth; cropHeight = (sourceWidth / ratio).toInt() }
        val scale = minOf(height.toFloat() / cropHeight, 1f)
        val bitmap: Bitmap
        val targetWidth: Int
        val targetHeight: Int
        if (scale < 1f) {
            val matrix = Matrix().apply { postScale(scale, scale) }
            bitmap = Bitmap.createBitmap(input, 0, 0, sourceWidth, sourceHeight, matrix, true)
            targetWidth = width; targetHeight = height
        } else { bitmap = input; targetWidth = cropWidth; targetHeight = cropHeight }
        return Prepared(bitmap, targetWidth, targetHeight, targetWidth / 2f - sourceWidth * scale / 2f,
            targetHeight / 2f - sourceHeight * scale / 2f)
    }

    private fun square(input: Bitmap?): Bitmap? {
        val prepared = prepare(input, 144, 144) ?: return null
        val output = Bitmap.createBitmap(prepared.width, prepared.height, Bitmap.Config.RGB_565)
        val canvas = Canvas(output)
        val paint = Paint(2).apply { isAntiAlias = true }
        canvas.drawColor(-1)
        canvas.drawBitmap(prepared.bitmap, prepared.left, prepared.top, paint)
        canvas.save()
        prepared.bitmap.recycle()
        return output
    }

    private fun rounded(input: Bitmap?, width: Int, height: Int, radius: Int): Bitmap? {
        val prepared = prepare(input, width, height) ?: return null
        val output = Bitmap.createBitmap(prepared.width, prepared.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint().apply { isAntiAlias = true }
        canvas.drawColor(0)
        canvas.drawRoundRect(RectF(Rect(0, 0, prepared.width, prepared.height)), radius.toFloat(), radius.toFloat(), paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(prepared.bitmap, prepared.left, prepared.top, paint)
        canvas.save()
        prepared.bitmap.recycle()
        return output
    }

    fun normalize(file: File): Boolean {
        if (!file.exists() || file.isDirectory) return false
        return try {
            val bitmap = square(BitmapFactory.decodeStream(FileInputStream(file))) ?: return false
            val output = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            output.flush()
            output.close()
            bitmap.recycle()
            true
        } catch (error: Exception) { NativeLog.error(null, error); false }
    }

    fun read(file: File, width: Int, height: Int, radius: Int): Bitmap? {
        if (!file.exists() || file.isDirectory) return null
        return try { rounded(BitmapFactory.decodeStream(FileInputStream(file)), width, height, radius) }
        catch (error: Exception) { NativeLog.error(null, error); null }
    }
}
