package moe.via.browser.feature.scan.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Shader
import android.view.View
import moe.via.browser.ui.components.dp

/** Dimensions and moving gradient from com.tuyafeng.scanner.MyViewfinderView. */
internal class ScannerViewfinder(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var side = 0
    private var lineY = 0
    private var running = false
    private var points: List<PointF> = emptyList()
    private var previousPoints: List<PointF> = emptyList()
    val frame: Rect get() {
        val edge = minOf(side, width, height).coerceAtLeast(0)
        return Rect((width - edge) / 2, (height - edge) / 2, (width + edge) / 2, (height + edge) / 2)
    }
    fun frameSize(pixels: Int) { side = pixels; lineY = 0; invalidate() }
    fun running(value: Boolean) { running = value; if (value) invalidate() }
    fun points(value: List<PointF>) { previousPoints = points; points = value.take(20); invalidate() }

    override fun onDraw(canvas: Canvas) {
        if (!running || side <= 0) return
        val frame = frame
        if (frame.isEmpty) return
        paint.shader = null; paint.color = Color.WHITE; paint.style = Paint.Style.STROKE; paint.strokeWidth = 0f
        canvas.drawRect(frame, paint)
        paint.style = Paint.Style.FILL
        val length = frame.width() * .06f
        val thickness = context.dp(2).toFloat()
        fun corner(x: Float, y: Float, horizontal: Int, vertical: Int) {
            canvas.drawRect(minOf(x, x + length * horizontal), minOf(y, y + thickness * vertical), maxOf(x, x + length * horizontal), maxOf(y, y + thickness * vertical), paint)
            canvas.drawRect(minOf(x, x + thickness * horizontal), minOf(y, y + length * vertical), maxOf(x, x + thickness * horizontal), maxOf(y, y + length * vertical), paint)
        }
        corner(frame.left.toFloat(), frame.top.toFloat(), 1, 1)
        corner(frame.right.toFloat(), frame.top.toFloat(), -1, 1)
        corner(frame.left.toFloat(), frame.bottom.toFloat(), 1, -1)
        corner(frame.right.toFloat(), frame.bottom.toFloat(), -1, -1)
        paint.color = 0x60000000
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
        canvas.drawRect(0f, frame.top.toFloat(), frame.left.toFloat(), frame.bottom + 1f, paint)
        canvas.drawRect(frame.right + 1f, frame.top.toFloat(), width.toFloat(), frame.bottom + 1f, paint)
        canvas.drawRect(0f, frame.bottom + 1f, width.toFloat(), height.toFloat(), paint)
        lineY += context.dp(2)
        if (lineY > frame.height()) lineY = 0
        paint.shader = LinearGradient(frame.left.toFloat(), (frame.top + lineY).toFloat(), frame.right.toFloat(), (frame.top + lineY).toFloat(),
            intArrayOf(0x00FFFFFF, Color.WHITE, 0x00FFFFFF), floatArrayOf(0f, .5f, 1f), Shader.TileMode.CLAMP)
        canvas.drawRect(frame.left.toFloat(), (frame.top + lineY).toFloat(), frame.right.toFloat(), (frame.top + lineY + context.dp(3)).toFloat(), paint)
        paint.shader = null
        // Default Native result-point color ai is #00ffbd21.
        paint.color = 0x00FFBD21
        points.forEach { canvas.drawCircle(it.x, it.y, 6f, paint) }
        previousPoints.forEach { canvas.drawCircle(it.x, it.y, 3f, paint) }
        postInvalidateDelayed(16L, frame.left, frame.top, frame.right, frame.bottom)
    }
}
