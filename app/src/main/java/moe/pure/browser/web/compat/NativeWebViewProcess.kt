package moe.pure.browser.web.compat

import android.app.Application
import android.os.Build
import android.webkit.WebView

/** BrowserApp.k assigns the actual process name before that process creates a WebView. */
internal object NativeWebViewProcess {
    fun configure(application: Application) {
        if (Build.VERSION.SDK_INT < 28) return
        val process = Application.getProcessName()
        if (process.isNullOrEmpty() || process == application.packageName) return
        WebView.setDataDirectorySuffix(process)
    }
}
