package moe.pure.browser.feature.scripts.runtime.http

import org.json.JSONObject

/** o5.c serializes normal, download-progress and upload-progress views of one response. */
internal class NativeGmHttpResponse(private val context: JSONObject?) {
    var finalUrl: String? = null
    var readyState = 0
    var headers: String? = null
    var responseType: String? = null
    var text: String? = null
    var dataUrl: String? = null
    var status = 0
    var statusText: String? = null
    var lengthComputable = false
    var loaded = 0L
    var total = 0L
    var progress = false
    var upload = false

    fun json(): JSONObject = JSONObject().apply {
        if (!upload) {
            put("finalUrl", finalUrl); put("readyState", readyState); put("status", status)
            put("statusText", statusText); put("responseHeaders", headers)
        }
        if (!progress && !upload) {
            responseType?.let { put("responseType", it.ifEmpty { "undefined" }) }
            put("responseDataUrl", dataUrl); put("responseText", text); put("context", context)
        } else {
            put("lengthComputable", lengthComputable); put("loaded", loaded); put("total", total)
        }
    }
}
