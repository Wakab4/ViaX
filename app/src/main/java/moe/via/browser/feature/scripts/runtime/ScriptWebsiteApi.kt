package moe.via.browser.feature.scripts.runtime

import android.webkit.URLUtil
import org.json.JSONObject
import java.util.Locale

/** i6.j0.d uses the same engine secret; generated website messages deliberately omit identifier. */
internal object ScriptWebsiteApi {
    fun injection(url: String, phase: Int, secret: String): String? {
        if (phase != 1 && phase != 2 || !supports(url)) return null
        val nativeName = NativeGmNames.prompt(secret)
        val quotedSecret = JSONObject.quote(secret)
        fun call(name: String) = "window.$nativeName(JSON.stringify({secret:$quotedSecret,name:'$name','arguments':{name:e,namespace:t}}),$quotedSecret)"
        return """
            (function () {let key = 'via-fake-tampermonkey';if (window[key]) {return;};
            if(!window.$nativeName) {const nativePrompt = window.via_gm && window.via_gm.call ? window.via_gm.call.bind(window.via_gm) : window.prompt;Object.defineProperty(window, '$nativeName', {value: nativePrompt,writable: false,configurable: false,enumerable: true});}
            try {window[key] = true;window.external.Tampermonkey = {
            getVersion: function(e) {e({version:'4.5.1', id: 'Via'});},
            openOptions: function(e, t) {${call("openOptions")};},
            isInstalled: function(e, t, n) {var v = ${call("isInstalled")};if(v === 'undefined') { return; };try {v = JSON.parse(v);} catch(e) {};n(v);}
            }} catch (err) {}})();
        """.trimIndent()
    }
    private fun supports(url: String): Boolean {
        if (!URLUtil.isNetworkUrl(url)) return false
        val scheme = url.indexOf("://")
        if (scheme < 0) return false
        val start = scheme + 3
        val end = url.indexOf('/', start).takeIf { it >= 0 } ?: url.length
        val authority = url.substring(start, end).lowercase(Locale.ROOT)
        return listOf("greasyfork.org", "userscript.zone", "openuserjs.org", "sleazyfork.org").any(authority::contains)
    }
}
