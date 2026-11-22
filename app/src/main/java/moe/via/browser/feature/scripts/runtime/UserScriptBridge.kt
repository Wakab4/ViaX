package moe.via.browser.feature.scripts.runtime

import android.webkit.JavascriptInterface
import android.webkit.WebView
import moe.via.browser.R

/** Owns one WebView's via_gm receiver, phase execution and disposal. */
class UserScriptBridge private constructor(private val view: WebView, actions: ScriptActions) {
    data class MenuCommand(val scriptKey: String, val label: String, val frameId: String? = null)
    @Volatile private var disposed = false
    private val engine = UserScriptEngine.get(view.context)
    private var frames: IframeUserScripts? = null
    private val requests = ScriptHttpRequests(view) { frameId, script -> frames?.execute(frameId, script) == true }
    private val calls = NativeGmCalls(view, engine, actions, requests)
    private val menus = NativeGmMenus(view, engine.secret)

    inner class Receiver {
        @JavascriptInterface fun call(message: String?, secret: String?): String? =
            if (disposed) null else calls.call(message, secret)

        @JavascriptInterface fun applies(identifier: String?, url: String?, secret: String?): Boolean {
            if (disposed || secret != engine.secret || identifier.isNullOrEmpty() || url.isNullOrEmpty() ||
                url.startsWith("file://", true) || !engine.enabled) return false
            val script = engine.execution(identifier) ?: return false
            return script.enabled && !script.noframes && script.applies(url)
        }
    }
    private fun runAt(phase: Int) {
        if (disposed) return
        val url = view.url ?: return
        if (engine.enabled)
            ScriptWebsiteApi.injection(url, phase, engine.secret)?.let { view.evaluateJavascript(it, null) }
        for (script in engine.matching(url, phase)) view.evaluateJavascript(engine.assemble(script), null)
    }

    companion object {
        fun install(view: WebView, actions: ScriptActions) {
            if (view.getTag(R.id.user_script_bridge) != null) return
            val bridge = UserScriptBridge(view, actions)
            view.setTag(R.id.user_script_bridge, bridge)
            view.addJavascriptInterface(bridge.Receiver(), "via_gm")
            bridge.frames = IframeUserScripts(view, bridge.engine)
        }
        fun runAt(view: WebView, phase: Int) { (view.getTag(R.id.user_script_bridge) as? UserScriptBridge)?.runAt(phase) }
        fun refresh(view: WebView) { (view.getTag(R.id.user_script_bridge) as? UserScriptBridge)?.frames?.refresh() }
        fun commands(view: WebView, completed: (List<MenuCommand>) -> Unit) {
            val bridge = view.getTag(R.id.user_script_bridge) as? UserScriptBridge
            if (bridge == null) completed(emptyList()) else bridge.menus.commands { commands ->
                completed(if (bridge.disposed) emptyList() else commands + bridge.frames?.commands().orEmpty())
            }
        }
        fun invoke(view: WebView, command: MenuCommand) {
            val bridge = (view.getTag(R.id.user_script_bridge) as? UserScriptBridge)?.takeUnless { it.disposed } ?: return
            if (command.frameId == null) bridge.menus.invoke(command)
            else bridge.frames?.execute(command.frameId, bridge.menus.invocation(command))
        }
        fun dispose(view: WebView) {
            (view.getTag(R.id.user_script_bridge) as? UserScriptBridge)?.let {
                it.disposed = true; it.calls.dispose()
                it.frames?.close(); it.frames = null
            }
            view.removeJavascriptInterface("via_gm"); view.setTag(R.id.user_script_bridge, null)
        }
    }
}
