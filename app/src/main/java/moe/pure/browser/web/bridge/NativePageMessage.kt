package moe.pure.browser.web.bridge

import org.json.JSONObject

/** s6.u.postMessage payloads; UI actions resolve the browser's current page when delivered. */
sealed interface NativePageMessage {
    data class Phase(val from: Int, val runAt: Int) : NativePageMessage
    data class MarkerFilter(val filter: String) : NativePageMessage
    data class MarkerSave(val host: String, val filter: String) : NativePageMessage
    data class Submit(val text: String) : NativePageMessage
    data class Suggest(val text: String) : NativePageMessage
    data class Download(val url: String) : NativePageMessage
    data object Translation : NativePageMessage
    data class PasswordFocus(val show: Boolean) : NativePageMessage
    class PasswordCapture(val url: String, val username: String, val password: String) : NativePageMessage {
        override fun toString() = "PasswordCapture"
    }

    companion object {
        internal fun read(value: JSONObject): NativePageMessage? = when (value.optInt("action")) {
            101 -> {
                val from = value.optInt("from")
                val runAt = value.optInt("runAt")
                if (from == 0 || runAt == 0) null else Phase(from, runAt)
            }
            102 -> MarkerFilter(value.optString("filter"))
            103 -> {
                val host = value.optString("host")
                val filter = value.optString("filter")
                if (host.isEmpty() || filter.isEmpty()) null else MarkerSave(host, filter)
            }
            105 -> value.optString("text").takeIf(String::isNotEmpty)?.let(::Submit)
            106 -> Suggest(value.optString("text"))
            107 -> value.optString("url").takeIf(String::isNotEmpty)?.let(::Download)
            108 -> Translation
            109 -> PasswordFocus(value.optInt("show") > 0)
            110 -> PasswordCapture(value.optString("url"), value.optString("user"), value.optString("pass"))
            else -> null
        }
    }
}
