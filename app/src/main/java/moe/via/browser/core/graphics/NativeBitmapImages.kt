package moe.via.browser.core.graphics

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** z8.i's sampling and viewport crop retain its float ratio and intermediate bitmap ownership. */
internal object NativeBitmapImages {
    fun sample(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
        var sample = 1
        if (width > 0 && height > 0 && targetWidth > 0 && targetHeight > 0) {
            if (height <= targetHeight && width <= targetWidth) return 1
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / sample >= targetHeight && halfWidth / sample >= targetWidth) sample *= 2
        }
        return sample
    }

    fun crop(bitmap: Bitmap?, width: Int, height: Int): Bitmap? {
        if (bitmap == null || bitmap.isRecycled || width <= 0 || height <= 0) return null
        val sourceWidth = bitmap.width
        val sourceHeight = bitmap.height
        if (sourceWidth <= 0 || sourceHeight <= 0) return null
        val ratio = width.toFloat() / height
        if (sourceWidth.toFloat() / sourceHeight > ratio) {
            val croppedWidth = minOf((sourceHeight * ratio).toInt(), sourceWidth)
            val cropped = Bitmap.createBitmap(bitmap, (sourceWidth - croppedWidth) / 2, 0, croppedWidth, sourceHeight)
            return if (sourceHeight > height) Bitmap.createScaledBitmap(cropped, width, height, true) else cropped
        }
        val croppedHeight = minOf((sourceWidth / ratio).toInt(), sourceHeight)
        val cropped = Bitmap.createBitmap(bitmap, 0, (sourceHeight - croppedHeight) / 2, sourceWidth, croppedHeight)
        return if (sourceWidth > width) Bitmap.createScaledBitmap(cropped, width, height, true) else cropped
    }

    fun decodeCrop(path: String?, width: Int, height: Int): Bitmap? {
        if (path.isNullOrEmpty() || width <= 0 || height <= 0) return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = sample(options.outWidth, options.outHeight, width, height)
        options.inJustDecodeBounds = false
        return crop(BitmapFactory.decodeFile(path, options), width, height)
    }
}
