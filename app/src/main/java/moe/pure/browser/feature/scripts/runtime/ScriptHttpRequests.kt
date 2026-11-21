package moe.pure.browser.feature.scripts.runtime

import android.webkit.WebView
import moe.pure.browser.feature.scripts.runtime.http.NativeGmHttpOptions
import moe.pure.browser.feature.scripts.runtime.http.NativeGmHttpRequest
import org.json.JSONException
import org.json.JSONObject
import java.lang.ref.WeakReference

/** NativeGmCalls has already checked the shared secret and stored grant before entering o5.b. */
class ScriptHttpRequests(private val view: WebView, private val executeInFrame: (String, String) -> Boolean) {
    fun request(details: String, frameId: String?) {
        val options = try { NativeGmHttpOptions(JSONObject(details), view) } catch (_: JSONException) { return }
        val target = WeakReference(view)
        NativeGmHttpRequest(options) { script ->
            view.post {
                val source = target.get() ?: return@post
                if (frameId == null) source.evaluateJavascript(script, null)
                else executeInFrame(frameId, script.removePrefix("javascript: "))
            }
        }.start()
    }
}
