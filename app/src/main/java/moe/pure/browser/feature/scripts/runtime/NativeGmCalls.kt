package moe.pure.browser.feature.scripts.runtime

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import moe.pure.browser.feature.scripts.model.ScriptIdentity
import org.json.JSONObject

/** o5.a's synchronous via_gm.call contract; browser UI actions remain with the existing host. */
internal class NativeGmCalls(private val view: WebView, private val engine: UserScriptEngine,
    private val actions: ScriptActions, private val requests: ScriptHttpRequests) {
    private val main = Handler(Looper.getMainLooper())
    private val resources = NativeGmResources(engine)

    fun call(message: String?, secret: String?): String? {
        if (secret.isNullOrEmpty() || secret != engine.secret || message.isNullOrEmpty()) return null
        return try {
            val request = JSONObject(message)
            if (request.optString("secret") != engine.secret) return null
            val name = request.optString("name")
            val identifier = request.optString("identifier")
            if (name.isEmpty() || identifier.isEmpty()) return null
            dispatch(name, identifier, request.optJSONObject("arguments"), request.optString("frameId").takeIf(String::isNotEmpty))
        } catch (error: Exception) {
            moe.pure.browser.core.logging.NativeLog.warn("UserScript", "GM call failed: ${error.javaClass.simpleName}")
            null
        }
    }

    private fun dispatch(name: String, identifier: String, arguments: JSONObject?, frameId: String?): String? {
        if (name == "info") return ScriptInfo.build(engine.repository.find(identifier))
        if (name == "isInstalled" && arguments != null) {
            val scriptName = arguments.optString("name")
            val script = engine.repository.find(ScriptIdentity.key(arguments.optString("namespace"), scriptName))
            return JSONObject().put("name", scriptName).put("installed", script != null).apply {
                if (script != null) put("enabled", script.enabled).put("version", script.version)
            }.toString()
        }
        if (name == "openOptions" && arguments != null) {
            moe.pure.browser.core.logging.NativeLog.debug("UserScript", "open options, name: ${arguments.optString("name")}, namespace: ${arguments.optString("namespace")}")
            return null
        }
        if (name != "listValues" && arguments == null) return null
        val mask = NativeGmGrants.mask(name)
        if (mask == 0) return null
        val grants = engine.execution(identifier)?.grant ?: 0
        if (grants and mask == 0) return NativeGmGrants.refused(name)
        if (name == "listValues") return engine.repository.valueNames(identifier).joinToString(",")
        val args = arguments!!
        when (name) {
            "setValue" -> engine.repository.setValue(identifier, args.optString("name"), args.optString("value", "undefined"))
            "getValue" -> return engine.repository.value(identifier, args.optString("name")) ?: args.optString("value", "undefined")
            "deleteValue" -> engine.repository.deleteValue(identifier, args.optString("name"))
            "getResourceText", "getResourceURL" -> return resources.read(identifier, args.optString("resource"), name == "getResourceURL")
            "log" -> moe.pure.browser.core.logging.NativeLog.debug("UserScript", "$identifier: ${args.optString("message")}")
            "xmlhttpRequest" -> requests.request(args.optString("details"), frameId)
            "openInTab" -> {
                val options = try { JSONObject(args.optString("options")) } catch (_: org.json.JSONException) { null }
                val url = args.optString("url")
                val active = options?.optBoolean("active", false) ?: false
                val insert = options?.optInt("insert", -1) ?: -1
                main.post { actions.open(url, active, insert) }
            }
            "download" -> {
                val url = args.optString("url")
                val filename = args.optString("name")
                main.post { actions.download(url, filename) }
            }
            "setClipboard" -> {
                val type = args.optString("type").let { if (it.isEmpty() || it == "undefined" || it == "null") "text/plain" else it }
                if (type.startsWith("text/") || type.equals("text", true))
                    view.context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, args.optString("data")))
            }
        }
        return null
    }

    fun dispose() { main.removeCallbacksAndMessages(null) }
}
