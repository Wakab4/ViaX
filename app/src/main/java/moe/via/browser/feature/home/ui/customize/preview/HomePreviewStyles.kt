package moe.via.browser.feature.home.ui.customize.preview

import android.content.Context
import android.webkit.WebResourceResponse
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException

/** d9.q.s3 intercepts only the original CSS suffix and keeps its response headers. */
internal object HomePreviewStyles {
    fun intercept(url: String?, context: () -> Context): WebResourceResponse? {
        if (url == null || !url.endsWith("homepage.css")) return null
        var response: WebResourceResponse? = null
        try {
            response = WebResourceResponse("text/css", "UTF-8", BufferedInputStream(FileInputStream(File(context().filesDir, "homepage.css").absolutePath)))
            response.responseHeaders = hashMapOf("Cache-Control" to "no-cache", "Access-Control-Allow-Origin" to "*", "Content-Type" to "text/css")
        } catch (error: FileNotFoundException) { error.printStackTrace() }
        return response
    }
}
