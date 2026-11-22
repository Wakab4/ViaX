package moe.via.browser.feature.scripts.runtime

import android.webkit.WebView
import org.json.JSONObject
import org.json.JSONTokener

/** i6.j0.j reads the shared menu dictionary in JS insertion order. */
internal class NativeGmMenus(private val view: WebView, private val secret: String) {
    fun commands(completed: (List<UserScriptBridge.MenuCommand>) -> Unit) {
        val key = JSONObject.quote(secret)
        view.evaluateJavascript("var es=window[$key]['gm_menus'];result={};for(var k in es){var items=es[k],list=[];for(var i in items)list.push(i);result[k]=list}JSON.stringify(result)") { raw ->
            val result = runCatching { JSONObject(JSONTokener(raw).nextValue() as String) }.getOrNull()
            completed(if (result == null) emptyList() else buildList {
                result.keys().forEach { identifier ->
                    val labels = result.getJSONArray(identifier)
                    for (index in 0 until labels.length()) add(UserScriptBridge.MenuCommand(identifier, labels.getString(index)))
                }
            })
        }
    }
    fun invoke(command: UserScriptBridge.MenuCommand) {
        view.evaluateJavascript(invocation(command), null)
    }
    fun invocation(command: UserScriptBridge.MenuCommand) =
        "(function(){var s=window[${JSONObject.quote(secret)}],m=s&&s.gm_menus&&s.gm_menus[${JSONObject.quote(command.scriptKey)}],f=m&&m[${JSONObject.quote(command.label)}];if(typeof f==='function')f()})()"
}
