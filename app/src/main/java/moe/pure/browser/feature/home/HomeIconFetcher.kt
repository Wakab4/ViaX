package moe.pure.browser.feature.home

import android.content.Context
import moe.pure.browser.data.BrowserExecutors
import moe.pure.browser.feature.home.icons.NativeHomeIconPaths
import moe.pure.browser.feature.home.icons.NativeTouchIconDownload

/** Touch icons are recovered on subsequent visits, without replacing a chosen custom image. */
object HomeIconFetcher {
    fun touch(context: Context, pageUrl: String, imageUrl: String) {
        if (imageUrl.contains("favicon.ico") || pageUrl.startsWith("file://", ignoreCase = true)) return
        val app = context.applicationContext
        BrowserExecutors.io.execute { NativeTouchIconDownload.obtain(NativeHomeIconPaths.directory(app), imageUrl, pageUrl) }
    }
}
