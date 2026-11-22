@file:Suppress("DEPRECATION")

package moe.via.browser.feature.scan.camera

import android.hardware.Camera
import kotlin.math.abs

/** Keeps the requested ratio between camera reopenings and maps it to supported zoom steps. */
internal class ScanCameraZoom {
    private var ratio = 100f

    fun restore(parameters: Camera.Parameters) {
        if (!parameters.isZoomSupported) return
        val ratios = parameters.zoomRatios
        if (ratios.size <= 1) return
        ratio = ratio.coerceIn(100f, ratios.last().toFloat())
        parameters.zoom = closest(ratios)
    }

    fun change(parameters: Camera.Parameters, factor: Float): Boolean {
        if (!parameters.isZoomSupported || !factor.isFinite() || factor <= 0f) return false
        val ratios = parameters.zoomRatios
        if (ratios.size <= 1) return false
        ratio = (ratio * factor).coerceIn(100f, ratios.last().toFloat())
        val target = closest(ratios)
        if (target == parameters.zoom) return false
        parameters.zoom = target
        return true
    }

    private fun closest(ratios: List<Int>) = ratios.indices.minBy { abs(ratios[it] - ratio) }
}
