package moe.via.browser.core.graphics

import android.content.Context
import android.graphics.Point
import android.view.WindowManager

/** g6.y.q/s use the real default-display size for background decoding. */
internal object NativeDisplaySize {
    fun real(context: Context) = Point(measure(context).x, measure(context).y)

    @Suppress("DEPRECATION")
    private fun measure(context: Context): Point = Point().also {
        val manager = context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.defaultDisplay.getRealSize(it)
    }
}
