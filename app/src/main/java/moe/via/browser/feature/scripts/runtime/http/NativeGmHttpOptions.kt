package moe.via.browser.feature.scripts.runtime.http

import android.webkit.WebSettings
import android.webkit.WebView
import org.json.JSONObject
import java.util.Locale

/** o5.b copies request details before starting a synchronous call or a new worker thread. */
internal class NativeGmHttpOptions(value: JSONObject, view: WebView) {
    val method = value.optString("method", "GET").uppercase(Locale.ROOT)
    val url = value.getString("url")
    val context = value.optJSONObject("context")
    val data = value.optString("data")
    val headers = value.optJSONObject("headers")
    val responseType = value.optString("responseType")
    val onError = value.optString("onerror")
    val onLoad = value.optString("onload")
    val onLoadStart = value.optString("onloadstart")
    val onProgress = value.optString("onprogress")
    val onReadyStateChange = value.optString("onreadystatechange")
    val onTimeout = value.optString("ontimeout")
    val overrideMimeType = value.optString("overrideMimeType")
    val synchronous = value.optBoolean("synchronous")
    val timeout = value.optInt("timeout")
    val upload = value.optJSONObject("upload")
    val user = value.optString("user")
    val password = value.optString("password")
    val cookie = value.optString("cookie")
    val anonymous = value.optBoolean("anonymous", false)
    val userAgent: String? = WebSettings.getDefaultUserAgent(view.context)
}
