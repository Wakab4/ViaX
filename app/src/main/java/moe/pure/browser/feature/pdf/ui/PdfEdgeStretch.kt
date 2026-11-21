package moe.pure.browser.feature.pdf.ui

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kotlin.math.abs

/** PdfViewer's bounded vertical stretch and .75/200 return spring. */
internal class PdfEdgeStretch(private val view: View) : AutoCloseable {
    private var spring: SpringAnimation? = null
    fun pull(delta: Float) {
        if (view.height <= 0) return
        spring?.cancel()
        view.pivotY = if (delta > 0) 0f else view.height.toFloat()
        val resistance = ((1.05f - view.scaleY) / .05f).coerceIn(0f, 1f)
        view.scaleY = (view.scaleY + abs(delta) / view.height * .15f * resistance).coerceIn(1f, 1.05f)
    }
    fun absorb(velocity: Float) {
        if (velocity == 0f || view.height == 0) return
        spring?.cancel()
        view.pivotY = if (velocity > 0) 0f else view.height.toFloat()
        view.scaleY = 1f + minOf(.05f, abs(velocity) / 20000f * .25f)
        release()
    }
    fun release() {
        if (view.scaleY == 1f || spring?.isRunning == true) return
        spring = SpringAnimation(view, DynamicAnimation.SCALE_Y).apply {
            spring = SpringForce(1f).setDampingRatio(.75f).setStiffness(200f)
            start()
        }
    }
    fun cancel() { spring?.cancel(); spring = null }
    override fun close() { cancel(); view.scaleY = 1f }
}
