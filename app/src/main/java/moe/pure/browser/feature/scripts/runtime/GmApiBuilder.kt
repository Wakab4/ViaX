package moe.pure.browser.feature.scripts.runtime

import android.content.Context
import moe.pure.browser.feature.scripts.model.ScriptGrants
import moe.pure.browser.feature.scripts.model.ScriptExecution
import org.json.JSONObject

/** Reference-generated permission fragments retain the original API aliases and Promise behavior. */
class GmApiBuilder(context: Context) {
    private val fragments = JSONObject(context.assets.open("scripts/gm-fragments.json").bufferedReader().use { it.readText() })
    fun build(script: ScriptExecution, secret: String): String = buildString {
        append(fragments.getString("base"))
        for (bit in ScriptGrants.bits.values.toSet()) if (bit != 0 && script.grant and bit != 0) append(fragments.getString(bit.toString()))
    }.replace("GM_SESSIONTOKENPLACEHOLDER_", NativeGmNames.identifier("GM_${secret}_"))
        .replace("NP_SESSIONTOKENPLACEHOLDER", NativeGmNames.prompt(secret))
        .replace("SCRIPTKEYPLACEHOLDER", script.key).replace("SESSIONTOKENPLACEHOLDER", secret)
}
