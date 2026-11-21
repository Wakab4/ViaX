package moe.pure.browser.feature.scan.camera

import android.graphics.PointF
import android.graphics.Rect
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.ResultPointCallback
import com.google.zxing.common.HybridBinarizer
import kotlin.math.max

internal data class ScanFrameGeometry(val viewportWidth: Int, val viewportHeight: Int, val frame: Rect)
internal data class ScanFrameResult(val text: String?, val points: List<PointF>)

/** Native t5.l(2052) enables CODE_39 and QR_CODE for camera frames. */
internal class ScanFrameDecoder {
    private val reader = MultiFormatReader()
    fun decode(bytes: ByteArray, width: Int, height: Int, rotation: Int, mirrored: Boolean, geometry: ScanFrameGeometry): ScanFrameResult {
        val displayWidth = if (rotation % 180 == 0) width else height
        val displayHeight = if (rotation % 180 == 0) height else width
        if (width <= 0 || height <= 0 || bytes.size.toLong() < width.toLong() * height || geometry.viewportWidth <= 0 || geometry.viewportHeight <= 0)
            return ScanFrameResult(null, emptyList())
        val rotated = rotate(bytes, width, height, rotation)
        val scale = max(geometry.viewportWidth.toFloat() / displayWidth, geometry.viewportHeight.toFloat() / displayHeight)
        val originX = (geometry.viewportWidth - displayWidth * scale) / 2f
        val originY = (geometry.viewportHeight - displayHeight * scale) / 2f
        val left = ((geometry.frame.left - originX) / scale).toInt().coerceIn(0, displayWidth - 1)
        val top = ((geometry.frame.top - originY) / scale).toInt().coerceIn(0, displayHeight - 1)
        val right = ((geometry.frame.right - originX) / scale).toInt().coerceIn(left + 1, displayWidth)
        val bottom = ((geometry.frame.bottom - originY) / scale).toInt().coerceIn(top + 1, displayHeight)
        val points = mutableListOf<PointF>()
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.CODE_39, BarcodeFormat.QR_CODE),
            DecodeHintType.NEED_RESULT_POINT_CALLBACK to ResultPointCallback { point ->
                val x = (left + point.x) * scale + originX
                points += PointF(if (mirrored) geometry.viewportWidth - x else x, (top + point.y) * scale + originY)
            }
        )
        val text = try {
            reader.setHints(hints)
            val source = PlanarYUVLuminanceSource(rotated, displayWidth, displayHeight, left, top, right - left, bottom - top, false)
            reader.decodeWithState(BinaryBitmap(HybridBinarizer(source))).text
        } catch (_: com.google.zxing.ReaderException) { null }
        finally { reader.reset() }
        return ScanFrameResult(text?.takeIf(String::isNotEmpty), points)
    }

    private fun rotate(data: ByteArray, width: Int, height: Int, rotation: Int): ByteArray {
        if (rotation == 0) return data
        val output = ByteArray(Math.multiplyExact(width, height))
        for (y in 0 until height) for (x in 0 until width) {
            val target = when (rotation) {
                90 -> x * height + height - y - 1
                180 -> (height - y - 1) * width + width - x - 1
                270 -> (width - x - 1) * height + y
                else -> y * width + x
            }
            output[target] = data[y * width + x]
        }
        return output
    }
}
