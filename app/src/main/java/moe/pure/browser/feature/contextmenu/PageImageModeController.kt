package moe.pure.browser.feature.contextmenu

import android.app.Activity
import android.webkit.WebView
import moe.pure.browser.ui.components.BrowserMessage

/** GP c8.kb.a -> s6.p8/m9. The page's popup path owns the newly generated Images document. */
class PageImageModeController(private val activity: Activity) {
    private val message = BrowserMessage(activity)
    private val script by lazy {
        activity.assets.open("contextmenu/image-mode.js").bufferedReader().use { it.readText() }
    }
    private var disposed = false

    fun show(view: WebView): Boolean {
        if (disposed) return false
        if (!view.settings.javaScriptEnabled) return false
        return runCatching {
            val source = script.replace("__NO_IMAGES__", "无图片").removePrefix("javascript:")
            view.evaluateJavascript(source, null)
            true
        }.getOrElse { message.show("操作失败"); false }
    }

    fun dispose() { disposed = true; message.close() }
}
