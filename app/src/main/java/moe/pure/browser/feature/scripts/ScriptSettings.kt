package moe.pure.browser.feature.scripts

import android.content.Context
import moe.pure.browser.feature.scripts.runtime.NativeFulltextScript
import moe.pure.browser.feature.scripts.runtime.UserScriptEngine
import moe.pure.browser.feature.settings.data.BrowserPreferences

object ScriptSettings {
    fun fulltext(context: Context): Boolean = NativeFulltextScript.enabled(UserScriptEngine.get(context).repository)
    fun fulltext(context: Context, enabled: Boolean) {
        BrowserPreferences(context.getSharedPreferences("browser", Context.MODE_PRIVATE)).fulltextExpansion = enabled
        NativeFulltextScript.update(context, UserScriptEngine.get(context).repository, enabled)
    }
}
