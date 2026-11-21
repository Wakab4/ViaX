package moe.pure.browser.ui.chrome

import android.app.Activity
import android.content.res.Configuration
import android.view.Gravity
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import moe.pure.browser.feature.settings.data.BrowserPreferences

enum class ChromeMode { TRADITIONAL, TOP, BOTTOM, DOUBLE_BOTTOM }

data class ChromeLayoutSpec(val mode: ChromeMode, val tablet: Boolean) {
    val combined: Boolean get() = mode == ChromeMode.TOP || mode == ChromeMode.BOTTOM
    val topAnchored: Boolean get() = mode == ChromeMode.TOP
    val popupGravity: Int get() = Gravity.END or if (topAnchored) Gravity.TOP else Gravity.BOTTOM

    fun popupWidth(activity: Activity): Int {
        val metrics = activity.resources.displayMetrics
        val shortSide = min(metrics.widthPixels, metrics.heightPixels)
        return if (tablet && mode != ChromeMode.TRADITIONAL) {
            min(shortSide - (96 * metrics.density).roundToInt(), (384 * metrics.density).roundToInt())
        } else shortSide
    }
}

object ChromeLayoutPolicy {
    fun resolve(activity: Activity): ChromeLayoutSpec {
        val metrics = activity.resources.displayMetrics
        val config = activity.resources.configuration
        val large = (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
        val diagonal = hypot(metrics.widthPixels / metrics.xdpi.toDouble(), metrics.heightPixels / metrics.ydpi.toDouble())
        val tablet = large && diagonal >= 7.0
        if ((activity as? BrowserWindowMode)?.customTabMode == true) return ChromeLayoutSpec(ChromeMode.TOP, tablet)
        val stored = BrowserPreferences(activity.getSharedPreferences("browser", Activity.MODE_PRIVATE)).toolbarLayout
        var mode = if (stored < 0) if (tablet) ChromeMode.TOP else ChromeMode.TRADITIONAL else ChromeMode.entries[stored]
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (mode == ChromeMode.TRADITIONAL) mode = ChromeMode.TOP
            if (mode == ChromeMode.DOUBLE_BOTTOM) mode = ChromeMode.BOTTOM
        }
        return ChromeLayoutSpec(mode, tablet)
    }
}
