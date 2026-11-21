package moe.pure.browser.feature.scripts.runtime

import android.webkit.WebView
import androidx.webkit.JavaScriptExecutionWorld
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import org.json.JSONObject

/** Keeps a frame-bound JavaScript target for asynchronous GM callbacks. */
internal class IframeFrameRegistry(private val view: WebView, private val world: JavaScriptExecutionWorld,
    private val engine: UserScriptEngine) : AutoCloseable {
    private val targets = LinkedHashMap<String, JavaScriptReplyProxy>()
    private val menus = LinkedHashMap<String, LinkedHashMap<String, UserScriptBridge.MenuCommand>>()
    private var closed = false
    private val bootstrap: ScriptHandler

    init {
        WebViewCompat.addWebMessageListener(view, BRIDGE, ORIGINS, world,
            WebViewCompat.WebMessageListener { source, message, _, mainFrame, reply ->
                if (closed || source !== view || mainFrame) return@WebMessageListener
                val data = runCatching { JSONObject(message.data.orEmpty()) }.getOrNull() ?: return@WebMessageListener
                if (data.optString("secret") != engine.secret) return@WebMessageListener
                val id = data.optString("id")
                if (id.length !in 12..80 || !id.all { it.isLetterOrDigit() || it == '-' }) return@WebMessageListener
                targets[id] = reply
                if (data.optString("type") == "menu") {
                    val key = data.optString("scriptKey")
                    val label = data.optString("label")
                    val script = engine.execution(key)
                    if (label.isNotEmpty() && label.length <= 100 && script?.enabled == true &&
                        script.grant and MENU_GRANTS != 0) {
                        menus.getOrPut(id, ::LinkedHashMap)["$key\u0000$label"] =
                            UserScriptBridge.MenuCommand(key, label, id)
                    }
                }
                if (targets.size > 64) {
                    val oldest = targets.keys.first()
                    targets.remove(oldest); menus.remove(oldest)
                }
            })
        val name = JSONObject.quote(NativeGmNames.prompt(engine.secret))
        val token = JSONObject.quote(engine.secret)
        val source = """
            (function(){
              if(window===window.top)return;
              var id=(window.crypto&&window.crypto.randomUUID)?window.crypto.randomUUID():
                Math.random().toString(36).slice(2)+Date.now().toString(36);
              Object.defineProperty(window,'__viaxFrameId',{value:id,writable:false,configurable:false});
              Object.defineProperty(window,$name,{value:function(message,key){
                try{var request=JSON.parse(message);request.frameId=id;
                  return window.via_gm.call(JSON.stringify(request),key)}catch(ignored){return null}
              },writable:false,configurable:false});
              window.$BRIDGE.postMessage(JSON.stringify({id:id,secret:$token}));
            })();
        """.trimIndent()
        bootstrap = WebViewCompat.addJavaScriptOnEvent(view, source,
            WebViewCompat.INJECTION_EVENT_DOCUMENT_START, ORIGINS, world)
    }

    fun execute(frameId: String, script: String): Boolean {
        if (closed) return false
        val target = targets[frameId] ?: return false
        return try { target.executeJavaScript(script, null); true }
        catch (_: RuntimeException) { targets.remove(frameId); menus.remove(frameId); false }
    }

    fun commands(): List<UserScriptBridge.MenuCommand> = menus.values.flatMap { it.values }

    override fun close() {
        if (closed) return
        closed = true
        bootstrap.remove()
        WebViewCompat.removeWebMessageListener(view, world, BRIDGE)
        targets.clear()
        menus.clear()
    }

    private companion object {
        const val BRIDGE = "viaxFrameBridge"
        const val MENU_GRANTS = 65536 or 536870912
        val ORIGINS = setOf("*")
    }
}
