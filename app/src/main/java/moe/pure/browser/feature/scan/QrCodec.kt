package moe.pure.browser.feature.scan

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import moe.pure.browser.feature.scan.decode.QrImageDecoder

object QrCodec {
    fun encode(text: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8", EncodeHintType.MARGIN to 2))
        val pixels = IntArray(size * size) { index -> if (matrix[index % size, index / size]) 0xFF000000.toInt() else -1 }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
    fun decode(bitmap: Bitmap): String? = QrImageDecoder.decode(bitmap)
}
