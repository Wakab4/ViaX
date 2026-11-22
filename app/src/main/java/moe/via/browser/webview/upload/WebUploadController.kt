package moe.via.browser.webview.upload

import android.app.Activity
import android.content.Intent
import android.net.Uri
import moe.via.browser.core.logging.NativeLog as Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

/** Owns the WebView's transient callback without copying or opening any selected document. */
internal class WebUploadController(private val activity: Activity) {
    private data class Request(val code: Int, val callback: ValueCallback<Array<Uri>>)
    private var pending: Request? = null
    private var disposed = false

    fun show(callback: ValueCallback<Array<Uri>>, params: WebChromeClient.FileChooserParams): Boolean {
        if (disposed || activity.isFinishing || activity.isDestroyed) {
            deliver(callback, null)
            return true
        }
        val previous = pending
        val request = Request(allocateCode(), callback)
        pending = request
        if (previous?.callback !== callback) previous?.let { deliver(it.callback, null) }
        if (pending !== request || disposed) return true
        try {
            @Suppress("DEPRECATION")
            activity.startActivityForResult(WebUploadIntent.chooser(activity, params), request.code)
        } catch (error: Exception) {
            Log.warn("WebUpload", "Cannot open the file selector", error)
            if (pending === request) { pending = null; deliver(callback, null) }
        }
        return true
    }

    fun result(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode !in FIRST_REQUEST..LAST_REQUEST) return false
        val request = pending?.takeIf { it.code == requestCode } ?: return true
        pending = null
        var selected: Array<Uri>? = null
        if (resultCode == Activity.RESULT_OK) {
            try {
                data?.clipData?.let { clip -> selected = Array(clip.itemCount) { clip.getItemAt(it).uri } }
            } catch (error: Exception) {
                Log.warn("WebUpload", "Cannot read the selected file references", error)
            }
        }
        if (selected.isNullOrEmpty()) selected = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
        deliver(request.callback, selected)
        return true
    }

    fun dispose() {
        if (disposed) return
        disposed = true
        val callback = pending
        pending = null
        callback?.let { deliver(it.callback, null) }
    }

    private fun deliver(callback: ValueCallback<Array<Uri>>, selected: Array<Uri>?) {
        try { callback.onReceiveValue(selected) }
        catch (error: RuntimeException) { Log.warn("WebUpload", "The document callback is no longer available", error) }
    }

    companion object {
        private const val FIRST_REQUEST = 0x6100
        private const val LAST_REQUEST = 0x7fff
        private var nextCode = FIRST_REQUEST
        private fun allocateCode(): Int = nextCode.also { nextCode = if (it == LAST_REQUEST) FIRST_REQUEST else it + 1 }
    }
}
