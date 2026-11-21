package moe.pure.browser.feature.scan.decode

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/** Native t5.z.c reads the complete bitmap with a QR-only reader and UTF-8 hints. The caller owns the bitmap. */
object QrImageDecoder {
    fun decode(bitmap: Bitmap): String? {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
        val pixels = IntArray(Math.multiplyExact(bitmap.width, bitmap.height))
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val binary = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bitmap.width, bitmap.height, pixels)))
        return runCatching { QRCodeReader().decode(binary, mapOf(
            DecodeHintType.CHARACTER_SET to "utf-8", DecodeHintType.TRY_HARDER to true,
        )).text }.getOrNull()
    }
}
