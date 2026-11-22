package moe.via.browser.feature.reader

import android.webkit.WebView
import java.lang.ref.WeakReference

/** i6.e holds the page weakly and reports adapter errors separately from JavaScript values. */
internal class ReaderPage(view: WebView) {
    private val reference = WeakReference(view)
    val url: String? get() = reference.get()?.url
    val javascriptEnabled: Boolean get() = reference.get()?.settings?.javaScriptEnabled ?: true

    fun evaluate(script: String?, completed: ((String?, String?) -> Unit)? = null) {
        val view = reference.get()
        if (view == null) completed?.invoke(null, "WebView reference is null")
        else if (script.isNullOrEmpty()) completed?.invoke(null, "Script is empty")
        else if (completed == null) view.evaluateJavascript(script, null)
        else view.evaluateJavascript(script) { raw -> completed(unwrap(raw), null) }
    }
    private fun unwrap(raw: String?): String? = if (raw != null && raw.length > 2 && raw[0] == '"')
        raw.substring(1, raw.length - 1).replace("\\\"", "\"").replace("\\\\", "\\").trim()
    else raw
}
