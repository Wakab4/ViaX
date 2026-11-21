package moe.pure.browser.web.compat

import android.content.Context

/** rc and s6.va keep their original viewport, notification and vibration snippets. */
internal class NativeHeadScripts(context: Context) {
    private val assets = context.applicationContext.assets
    private val desktop by lazy { read("desktop-viewport.js").replace("__WIDTH__", "1280") }
    private val zoom by lazy { read("force-zoom.js") }
    val notification by lazy { read("notification.js") }
    val vibration by lazy { read("vibrate.js") }

    fun viewport(host: String, desktopEnabled: Boolean, forceZoom: Boolean) = when {
        host != "music.163.com" && host != "taobao.com" && desktopEnabled -> desktop
        forceZoom -> zoom
        else -> ""
    }
    private fun read(name: String) = assets.open("web/head/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }
}
