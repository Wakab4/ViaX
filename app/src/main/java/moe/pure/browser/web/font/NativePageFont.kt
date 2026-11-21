package moe.pure.browser.web.font

import android.content.Context
import android.webkit.WebResourceResponse
import moe.pure.browser.feature.fonts.NativeFontStore
import java.net.URLEncoder

/** ab.a/ua.E0/s9.d.f share uifont with the original suffix request and font directory. */
internal class NativePageFont(context: Context) {
    private val app = context.applicationContext
    private val template by lazy { app.assets.open("web/head/custom-font.js").bufferedReader(Charsets.UTF_8).use { it.readText() } }
    private var suffix = ""

    fun selected(name: String) {
        suffix = if (name.isEmpty()) "" else try { URLEncoder.encode(name, "UTF-8").replace("+", "%20") }
            catch (_: Exception) { name }
    }
    fun script(name: String) = if (name.isEmpty()) "" else template.replace("__PATH__", "/$name")
    fun matches(url: String) = suffix.isNotEmpty() && url.endsWith(suffix)
    fun response(name: String): WebResourceResponse? = NativeFontStore.get(app).response(name)
}
