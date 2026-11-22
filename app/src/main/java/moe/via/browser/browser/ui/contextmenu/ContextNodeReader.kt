package moe.via.browser.browser.ui.contextmenu

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import java.lang.ref.WeakReference

/** WebView's native focus-node result retains both an image src and its enclosing anchor href. */
internal class ContextNodeReader {
    private val handler = Handler(Looper.getMainLooper())
    private var generation = 0

    fun read(view: WebView, fallbackLink: String?, fallbackImage: String?, result: (String?, String?, String?) -> Unit) {
        val request = ++generation
        val reference = WeakReference(view)
        val url = view.url
        val receiver = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: android.os.Message) {
                val current = reference.get() ?: return
                if (generation != request || current.url != url) return
                val data = message.data
                result(data.getString("url") ?: fallbackLink, data.getString("src") ?: fallbackImage, data.getString("title"))
            }
        }
        try { view.requestFocusNodeHref(receiver.obtainMessage()) }
        catch (_: RuntimeException) {
            handler.post { if (generation == request && reference.get()?.url == url) result(fallbackLink, fallbackImage, null) }
        }
    }

    fun cancel() { generation++; handler.removeCallbacksAndMessages(null) }
}
