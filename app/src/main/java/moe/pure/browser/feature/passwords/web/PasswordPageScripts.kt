package moe.pure.browser.feature.passwords.web

import android.webkit.WebView
import android.content.Context
import org.json.JSONObject

/** mb.b runs at the head phase; its form events use the root's shared via interface. */
internal object PasswordPageScripts {
    fun captureScript(context: Context, secret: String): String {
        val script = context.assets.open("passwords/capture.js").bufferedReader(Charsets.UTF_8).use { it.readText() }
        return script.replace("__VIA_PASSWORD_SECRET__", JSONObject.quote(secret))
    }

    fun fill(view: WebView, username: String, password: String, onResult: (Boolean) -> Unit) {
        val script = view.context.assets.open("passwords/fill.js").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arguments = JSONObject().put("user", username).put("pass", password).toString()
            .replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
        view.evaluateJavascript(script.replace("__PURE_PASSWORD_ARGUMENTS__", arguments)) { result ->
            when (result) { "0" -> onResult(false); "1" -> onResult(true) }
        }
    }
}
