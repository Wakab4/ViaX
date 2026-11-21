package moe.pure.browser.feature.scripts.runtime

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.webkit.JavaScriptExecutionWorld
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import moe.pure.browser.feature.scripts.model.ScriptExecution
import moe.pure.browser.feature.settings.data.BrowserPreferences
import org.json.JSONObject

/** Registers user scripts before navigation so each matching iframe gets its own execution context. */
internal class IframeUserScripts(private val view: WebView, private val engine: UserScriptEngine) : AutoCloseable {
    private val main = Handler(Looper.getMainLooper())
    private val handlers = ArrayList<ScriptHandler>()
    private val preferences = view.context.getSharedPreferences("browser", Context.MODE_PRIVATE)
    private val modern = WebViewFeature.isFeatureSupported(WebViewFeature.JS_INJECTION_IN_FRAME_AND_WORLD)
    private val documentStart = modern || WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
    private val world = if (modern) WebViewCompat.getExecutionWorld(view, JavaScriptExecutionWorld.PAGE_WORLD_NAME) else null
    private val frameRegistry = if (modern) IframeFrameRegistry(view, requireNotNull(world), engine) else null
    private var closed = false
    private val refreshTask = Runnable { refresh() }
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == BrowserPreferences.WEB) scheduleRefresh()
    }
    private val cacheSubscription = engine.observeScripts(::scheduleRefresh)

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        refresh()
    }

    private fun scheduleRefresh() {
        main.removeCallbacks(refreshTask)
        main.post(refreshTask)
    }

    fun refresh() {
        if (closed) return
        handlers.forEach(ScriptHandler::remove)
        handlers.clear()
        if (!documentStart || !engine.enabled) return
        for (script in engine.scripts()) {
            if (!script.enabled || script.noframes) continue
            for (phase in intArrayOf(1, 2, 4)) {
                if (script.runAt and phase == 0) continue
                val source = if (modern && phase != 4) guarded(script) else deferred(script, phase)
                val handler = if (modern) WebViewCompat.addJavaScriptOnEvent(
                    view, source,
                    if (phase == 1) WebViewCompat.INJECTION_EVENT_DOCUMENT_START else WebViewCompat.INJECTION_EVENT_DOCUMENT_END,
                    ORIGINS, requireNotNull(world)
                ) else WebViewCompat.addDocumentStartJavaScript(view, source, ORIGINS)
                handlers.add(handler)
            }
        }
    }

    fun execute(frameId: String, script: String): Boolean = frameRegistry?.execute(frameId, script) == true
    fun commands(): List<UserScriptBridge.MenuCommand> = frameRegistry?.commands().orEmpty()

    private fun guarded(script: ScriptExecution): String {
        val key = JSONObject.quote(script.key)
        val secret = JSONObject.quote(engine.secret)
        val hook = if (modern && script.grant and MENU_GRANTS != 0) menuHook(key, secret) else null
        return "if(window!==window.top&&window.via_gm&&window.via_gm.applies($key,location.href,$secret)){\n" +
            engine.assemble(script, hook) + "\n}"
    }

    private fun menuHook(key: String, secret: String): String {
        val function = NativeGmNames.identifier("GM_${engine.secret}_") + "registerMenuCommand"
        return """
            var __viaxOriginalMenu=$function;
            $function=function(name,callback,menuKey){
              var result=__viaxOriginalMenu(name,callback,menuKey);
              if(result&&window.__viaxFrameId&&window.viaxFrameBridge)
                window.viaxFrameBridge.postMessage(JSON.stringify({type:'menu',id:window.__viaxFrameId,
                  secret:$secret,scriptKey:$key,label:name}));
              return result;
            };
            if(typeof GM_registerMenuCommand==='function')GM_registerMenuCommand=$function;
            if(typeof GM.registerMenuCommand==='function')GM.registerMenuCommand=$function;
        """.trimIndent()
    }

    private fun deferred(script: ScriptExecution, phase: Int): String {
        if (phase == 1) return guarded(script)
        val run = "function(){\n${guarded(script)}\n}"
        return if (phase == 2) """
            if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',$run,{once:true});
            else ($run)();
        """.trimIndent() else """
            if(document.readyState==='complete')setTimeout($run,0);
            else window.addEventListener('load',$run,{once:true});
        """.trimIndent()
    }

    override fun close() {
        if (closed) return
        closed = true
        cacheSubscription.close()
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        main.removeCallbacks(refreshTask)
        handlers.forEach(ScriptHandler::remove)
        handlers.clear()
        frameRegistry?.close()
    }

    private companion object { val ORIGINS = setOf("*"); const val MENU_GRANTS = 65536 or 536870912 }
}
