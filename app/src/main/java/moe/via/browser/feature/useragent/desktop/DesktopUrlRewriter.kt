package moe.via.browser.feature.useragent.desktop

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Point
import kotlin.math.pow
import kotlin.math.sqrt

/** GP i6.h.m/n/s: ordered string rules, with null meaning that the caller should reload. */
object DesktopUrlRewriter {
    fun rewrite(url: String, desktop: Boolean): String? {
        if (url.isEmpty()) return null
        return if (desktop) {
            BaiduDesktopUrls.search(url)
                ?: BaiduDesktopUrls.diskToDesktop(url)
                ?: WeiboDesktopUrls.toDesktop(url)
                ?: PortalDesktopUrls.toutiaoToDesktop(url)
                ?: ForumDesktopUrls.hupuToDesktop(url)
                ?: ForumDesktopUrls.ithomeToDesktop(url)
                ?: PortalDesktopUrls.copyrightToDesktop(url)
                ?: BaiduDesktopUrls.dictionaryToDesktop(url)
                ?: PortalDesktopUrls.mailToDesktop(url)
                ?: LiteralDesktopRules.rewrite(url, true)
        } else {
            BaiduDesktopUrls.diskToMobile(url)
                ?: WeiboDesktopUrls.toMobile(url)
                ?: PortalDesktopUrls.toutiaoToMobile(url)
                ?: ForumDesktopUrls.hupuToMobile(url)
                ?: ForumDesktopUrls.ithomeToMobile(url)
                ?: PortalDesktopUrls.copyrightToMobile(url)
                ?: PortalDesktopUrls.youtubeToMobile(url)
                ?: BaiduDesktopUrls.dictionaryToMobile(url)
                ?: PortalDesktopUrls.mailToMobile(url)
                ?: LiteralDesktopRules.rewrite(url, false)
        }
    }

    /** g6.y.E/r uses real display pixels and physical DPI, in addition to screenLayout. */
    @Suppress("DEPRECATION")
    fun tablet(activity: Activity): Boolean {
        val size = activity.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        if (size != Configuration.SCREENLAYOUT_SIZE_LARGE && size != Configuration.SCREENLAYOUT_SIZE_XLARGE) return false
        val metrics = activity.resources.displayMetrics
        val pixels = Point()
        activity.windowManager.defaultDisplay.getRealSize(pixels)
        val width = (pixels.x / metrics.xdpi).toDouble()
        val height = (pixels.y / metrics.ydpi).toDouble()
        return sqrt(width.pow(2.0) + height.pow(2.0)) >= 7.0
    }
}
