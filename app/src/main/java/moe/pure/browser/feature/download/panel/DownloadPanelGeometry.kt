package moe.pure.browser.feature.download.panel

import android.app.Activity
import android.view.Gravity
import moe.pure.browser.ui.BrowserChrome
import moe.pure.browser.ui.chrome.ChromeLayoutPolicy
import moe.pure.browser.ui.chrome.ChromeMode
import moe.pure.browser.ui.components.dp

/** s6.B8/Wa uses the browser root's short side, while the panel itself fills the content height. */
internal data class DownloadPanelGeometry(val width: Int, val gravity: Int, val height: Int = -1) {
    val top get() = gravity and Gravity.BOTTOM != Gravity.BOTTOM
    companion object {
        fun resolve(activity: Activity, chrome: BrowserChrome): DownloadPanelGeometry {
            val spec = ChromeLayoutPolicy.resolve(activity)
            val measured = minOf(chrome.root.width, chrome.root.height)
            val metrics = activity.resources.displayMetrics
            val short = if (measured > 0) measured else minOf(metrics.widthPixels, metrics.heightPixels)
            val width = if (spec.tablet && spec.mode != ChromeMode.TRADITIONAL) minOf(short - activity.dp(96), activity.dp(384)) else short
            return DownloadPanelGeometry(width.coerceAtLeast(1), Gravity.END or if (spec.mode == ChromeMode.TOP) Gravity.TOP else Gravity.BOTTOM)
        }
    }
}
