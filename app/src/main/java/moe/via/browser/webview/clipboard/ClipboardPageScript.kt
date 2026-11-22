package moe.via.browser.webview.clipboard

import android.content.Context
import moe.via.browser.R
import org.json.JSONObject

/** c8.ya's original scripts. They use engine clipboard/copy APIs and expose no Android clipboard bridge. */
object ClipboardPageScript {
    private data class Sources(val ask: String, val deny: String, val allow: String)
    @Volatile private var sources: Sources? = null

    fun create(context: Context, decision: Int): String {
        val scripts = sources ?: synchronized(this) {
            sources ?: Sources(read(context, "ask"), read(context, "deny"), read(context, "allow")).also { sources = it }
        }
        return when (decision) {
            2 -> scripts.deny
            3 -> {
                val quoted = JSONObject.quote(context.getString(R.string.ref_clipboard_confirm_message))
                    .replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
                scripts.ask.replace("%CONFIRM_MESSAGE%", quoted.substring(1, quoted.length - 1))
            }
            else -> scripts.allow
        }
    }
    private fun read(context: Context, name: String) = context.applicationContext.assets.open("clipboard/$name.js").bufferedReader(Charsets.UTF_8).use { it.readText() }
}
