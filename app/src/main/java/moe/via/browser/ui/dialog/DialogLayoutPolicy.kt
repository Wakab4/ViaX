package moe.via.browser.ui.dialog

import android.app.Activity
import android.os.Build
import android.view.Gravity
import android.view.WindowInsets
import android.view.WindowManager
import moe.via.browser.ui.theme.window.ChromeLayoutPolicy
import moe.via.browser.ui.components.dp

data class DialogGeometry(val width: Int, val contentHeightLimit: Int)
data class DialogAnchor(val x: Int, val y: Int)
data class AnchoredDialogGeometry(val width: Int, val contentHeightLimit: Int, val gravity: Int, val x: Int, val y: Int)

object DialogLayoutPolicy {
    fun centered(activity: Activity, form: Boolean): DialogGeometry {
        val content = activity.findViewById<android.view.View>(android.R.id.content)
        val metrics = activity.resources.displayMetrics
        val width = content?.width?.takeIf { it > 0 } ?: metrics.widthPixels
        val height = content?.height?.takeIf { it > 0 } ?: metrics.heightPixels
        val columns = if (form) 8 else 6
        var result = minOf(width, height) - activity.dp(72)
        if ((ChromeLayoutPolicy.resolve(activity).tablet || width > height) && result > activity.dp(columns * 60)) result = activity.dp((columns - 1) * 60)
        return DialogGeometry(result.coerceAtLeast(1), (height / 7) * 4)
    }
    fun anchored(activity: Activity, form: Boolean, point: DialogAnchor, requestedWidthPx: Int? = null): AnchoredDialogGeometry {
        val content = activity.findViewById<android.view.View>(android.R.id.content)
        val metrics = activity.resources.displayMetrics
        val width = content?.width?.takeIf { it > 0 } ?: metrics.widthPixels
        val height = content?.height?.takeIf { it > 0 } ?: metrics.heightPixels
        val popupLimit = minOf(activity.dp(240), (minOf(width, height) / 7) * if (form) 5 else 4)
        val popupWidth = requestedWidthPx?.let { minOf(it, popupLimit) } ?: popupLimit
        var x = if (point.x > width / 2) point.x - popupWidth else point.x
        if (x <= 10) x = activity.dp(12)
        else if (x + popupWidth >= width - 10) x = width - popupWidth - activity.dp(12)
        val bottom = point.y > height / 2
        var y = if (bottom) height - point.y else point.y
        val location = IntArray(2)
        content?.getLocationOnScreen(location)
        if (location[1] > 0) y += if (bottom) location[1] else -location[1]
        if (Build.VERSION.SDK_INT >= 30) {
            val insets = activity.getSystemService(WindowManager::class.java).currentWindowMetrics.windowInsets
            val bars = insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            if (insets.isVisible(WindowInsets.Type.statusBars()) || Build.VERSION.SDK_INT >= 35 && !bottom || Build.VERSION.SDK_INT < 35 && bottom) y -= if (bottom) bars.bottom else bars.top
        }
        return AnchoredDialogGeometry(popupWidth.coerceAtLeast(1), height * 3 / 5,
            Gravity.LEFT or if (bottom) Gravity.BOTTOM else Gravity.TOP, x, y)
    }
}
