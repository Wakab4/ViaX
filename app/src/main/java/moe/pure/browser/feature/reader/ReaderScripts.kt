package moe.pure.browser.feature.reader

import android.content.Context

/** Reader DOM assets and appearance are independent from browser chrome and settings screens. */
class ReaderScripts(context: Context) {
    private val assets = context.applicationContext.assets
    private val enter by lazy { asset("enter.js") }
    private val exit by lazy { asset("exit.js") }
    private val state by lazy { asset("state.js") }
    private val active by lazy { asset("active.js") }
    private val base by lazy { asset("base.css") }

    fun probeState(url: String) = if (ReaderProbeRules.restricted(url)) active else state
    fun baseStyle() = style("__via_reader__", base)
    fun enter() = enter
    fun exit() = exit
    fun active() = active

    fun appearance(preferences: ReaderPreferences): String = ReaderAppearance.from(preferences).script()

    private fun style(id: String, content: String) = "(function(){var e=\"$content\",t=document.getElementById(\"$id\");" +
        "if(t)t.innerText=e;else{(t=document.createElement(\"style\")).type=\"text/css\";t.charset=\"UTF-8\";" +
        "t.id=\"$id\";t.appendChild(document.createTextNode(e));document.head.appendChild(t)}})();"

    private fun asset(name: String) = assets.open("reader/$name").bufferedReader().use { it.readText() }

}
