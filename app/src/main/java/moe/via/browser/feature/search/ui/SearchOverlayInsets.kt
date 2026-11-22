package moe.via.browser.feature.search.ui

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager

/** Intersects the host's actual screen rectangle with safe/IME bounds to avoid applying insets twice. */
internal object SearchOverlayInsets {
    fun apply(activity: Activity, layer: View, host: View, insets: WindowInsets?) {
        if (host.width <= 0 || host.height <= 0) return
        val location = IntArray(2)
        host.getLocationOnScreen(location)
        val safe = if (Build.VERSION.SDK_INT >= 30 && insets != null) {
            val bounds = activity.getSystemService(WindowManager::class.java).currentWindowMetrics.bounds
            val system = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val ime = insets.getInsets(WindowInsets.Type.ime())
            Rect(bounds.left + system.left, bounds.top + system.top, bounds.right - system.right,
                bounds.bottom - maxOf(system.bottom, ime.bottom))
        } else Rect().also { host.getWindowVisibleDisplayFrame(it) }
        val left = (safe.left - location[0]).coerceAtLeast(0)
        val top = (safe.top - location[1]).coerceAtLeast(0)
        val right = (location[0] + host.width - safe.right).coerceAtLeast(0)
        val bottom = (location[1] + host.height - safe.bottom).coerceAtLeast(0)
        if (layer.paddingLeft != left || layer.paddingTop != top || layer.paddingRight != right || layer.paddingBottom != bottom)
            layer.setPadding(left, top, right, bottom)
    }
}
